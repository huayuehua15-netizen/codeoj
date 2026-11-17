#!/bin/bash
# =====================================================================
# security-e2e-test.sh —— 恶意样例全链路测试（论文 6.3 / 表 6-2）
#
# 前置条件：
#   1. docker compose 栈运行中，oj-judge 使用 ENGINE_MODE=real（真实沙箱判题）；
#   2. 五语言判题镜像已构建并拉取到判题节点；
#   3. 存在至少一道允许 c/cpp/python3 的题目（默认取第一道，可用 PROBLEM_ID 指定）；
#   4. 测试主机具备 python3（用于 JSON 转义）与 curl。
#
# 用法：
#   BASE_URL=http://localhost:8080/api ./security-e2e-test.sh
#   PROBLEM_ID=1 BASE_URL=http://localhost:8080/api ./security-e2e-test.sh
#
# 原理：经真实提交接口提交恶意样例，轮询判题结果，断言状态机映射正确：
#   fork 炸弹/死循环 → TLE；内存炸弹 → MLE；磁盘/输出攻击 → RE；
#   网络/宿主读取/提权 → 样例可运行但输出不匹配 → WA（隔离生效的标志）；
#   编译期攻击 → CE；SQL 注入 → 平台正常处理不崩溃。
# =====================================================================
set -u
cd "$(dirname "$0")"

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
USERNAME="${TEST_USERNAME:-sectest_$(date +%s)}"
export TEST_PASSWORD="${TEST_PASSWORD:-SecTest123!}"
TOKEN="${TEST_TOKEN:-}"

FAIL=0
TOTAL=0
PASS=0

command -v curl >/dev/null 2>&1 || { echo "缺少 curl"; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "缺少 python3（用于 JSON 转义）"; exit 2; }

get()  { curl -sS --max-time 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL$1"; }
post() { curl -sS --max-time 60 -H "Content-Type: application/json" \
           -H "Authorization: Bearer $TOKEN" -d "$2" "$BASE_URL$1"; }

# 注册成功不签发 token；总是显式登录。已有账号/预发 token 可重复测试而不消耗注册配额。
if [ -z "$TOKEN" ]; then
  auth_args=()
  [ -z "${TEST_USERNAME:-}" ] || auth_args+=(--existing)
  TOKEN=$(python3 auth-token.py --base "$BASE_URL" --username "$USERNAME" "${auth_args[@]}") || exit 2
fi

# 题目发现
if [ -z "${PROBLEM_ID:-}" ]; then
  PROBLEM_ID=$(get "/problems?page=1&size=1" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)
fi
[ -n "$PROBLEM_ID" ] || { echo "未找到题目，请用 PROBLEM_ID 指定"; exit 2; }
echo "使用题目 #$PROBLEM_ID，测试账号 $USERNAME"

submit_code() { # $1=language $2=source-file → 输出 submissionId
  local code_json
  code_json=$(python3 -c 'import json,sys;print(json.dumps(open(sys.argv[1],encoding="utf-8").read()))' "$2")
  post "/submissions" "{\"problemId\":$PROBLEM_ID,\"language\":\"$1\",\"code\":$code_json}"
}

wait_status() { # $1=submissionId → 输出终态(2..8) 或 timeout
  local st response deadline_at=$((SECONDS + 120))
  while [ "$SECONDS" -lt "$deadline_at" ]; do
    response=$(get "/submissions/$1")
    if [ -n "${EVIDENCE_DIR:-}" ]; then
      mkdir -p "$EVIDENCE_DIR"
      printf '%s\n' "$response" > "$EVIDENCE_DIR/submission-$1.json"
    fi
    st=$(printf '%s' "$response" | python3 -c 'import json,sys; r=json.load(sys.stdin); d=r.get("data"); print(d.get("status", "") if r.get("code")==0 and isinstance(d,dict) else "")' 2>/dev/null)
    if [ -n "$st" ] && [ "$st" -ge 2 ]; then
      echo "$st"
      return 0
    fi
    sleep 2
  done
  echo "timeout"
  return 1
}

check_case() { # $1=id $2=语言 $3=样例文件 $4=期望状态 $5=描述
  TOTAL=$((TOTAL + 1))
  local resp sid st
  resp=$(submit_code "$2" "$3")
  sid=$(echo "$resp" | grep -o '"submissionId":[0-9]*' | cut -d: -f2)
  if [ -z "$sid" ]; then
    FAIL=$((FAIL + 1))
    printf '  \033[31mFAIL\033[0m  %-8s 提交失败: %s\n' "$1" "$(echo "$resp" | head -c 120)"
    return
  fi
  st=$(wait_status "$sid")
  if [ "$st" = "$4" ]; then
    PASS=$((PASS + 1))
    printf '  \033[32mPASS\033[0m  %-8s submission=%-6s status=%s（%s）\n' "$1" "$sid" "$st" "$5"
  else
    FAIL=$((FAIL + 1))
    printf '  \033[31mFAIL\033[0m  %-8s submission=%-6s status=%s 期望=%s（%s）\n' "$1" "$sid" "$st" "$4" "$5"
  fi
}

echo "=============================================================="
echo " 恶意样例全链路测试（论文表 6-2）｜ BASE_URL=$BASE_URL"
echo "=============================================================="

# 状态码：0排队 1判题中 2AC 3WA 4TLE 5MLE 6RE 7CE 8SE
# 注意：提交限流 6 次/分/用户，同一测试账号相邻提交间隔 15s
check_case s01 c   "malicious/s01-fork-bomb/c/main.c"        4 "fork 炸弹 → TLE"
sleep 15
check_case s02 c   "malicious/s02-dead-loop/c/main.c"         4 "死循环 → TLE"
sleep 15
check_case s03 c   "malicious/s03-memory-bomb/c/main.c"       5 "内存炸弹 → MLE"
sleep 15
check_case s03-py python3 "malicious/s03-memory-bomb/python3/main.py" 5 "Python 内存炸弹 → MLE"
sleep 15
check_case s04 c   "malicious/s04-disk-fill/c/main.c"         6 "磁盘写满 → RE(SIGXFSZ)"
sleep 15
check_case s05 c   "malicious/s05-network/c/main.c"           3 "网络访问：隔离生效则程序可运行→WA；意外连通会变 RE"
sleep 15
check_case s06 c   "malicious/s06-host-info/c/main.c"         3 "读取宿主机：敏感路径被阻断→WA；泄露会变 RE"
sleep 15
check_case s07 c   "malicious/s07-privilege/c/main.c"         3 "提权尝试：setuid 被拒→WA；成功提权会变 RE"
sleep 15
check_case s08 c   "malicious/s08-output-bomb/c/main.c"       6 "输出轰炸 → RE(16MB 限制)"
sleep 15
check_case s10 cpp "malicious/s10-compile-bomb/cpp/main.cpp"  7 "编译期攻击 → CE(编译容器被杀)"
sleep 15

# S-09 SQL 注入（应用层）：提交注入载荷代码，平台必须正常处理（不崩溃、不泄露）
TOTAL=$((TOTAL + 1))
RESP=$(submit_code c "malicious/s09-sql-injection/c/main.c")
SID=$(echo "$RESP" | grep -o '"submissionId":[0-9]*' | cut -d: -f2)
ST=timeout
[ -z "$SID" ] || ST=$(wait_status "$SID")
if [ "$ST" = "2" ] || [ "$ST" = "3" ] || [ "$ST" = "7" ]; then
  PASS=$((PASS + 1))
  printf '  \033[32mPASS\033[0m  %-8s status=%s 注入载荷按普通代码处理\n' s09 "$ST"
else
  FAIL=$((FAIL + 1))
  printf '  \033[31mFAIL\033[0m  %-8s status=%s 出现异常状态\n' s09 "$ST"
fi

# S-09 SQL 注入（应用层）：搜索接口注入串
TOTAL=$((TOTAL + 1))
SRESP=$(get "/problems?keyword=%27%20OR%20%271%27%3D%271")
if echo "$SRESP" | grep -q '"code":0'; then
  PASS=$((PASS + 1))
  printf '  \033[32mPASS\033[0m  %-8s 搜索接口对注入串正常返回\n' "s09-kw"
else
  FAIL=$((FAIL + 1))
  printf '  \033[31mFAIL\033[0m  %-8s 搜索接口异常: %s\n' "s09-kw" "$(echo "$SRESP" | head -c 120)"
fi

echo "=============================================================="
echo " 总计: $TOTAL  通过: $PASS  失败: $FAIL"
echo "=============================================================="
exit $((FAIL > 255 ? 255 : FAIL))
