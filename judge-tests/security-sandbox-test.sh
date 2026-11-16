#!/bin/bash
# =====================================================================
# security-sandbox-test.sh —— 恶意样例沙箱直测（论文 6.3 / 表 6-2）
#
# 前置条件：Linux + Docker Engine + 四语言判题镜像已构建（judge-images/）
# 用法：    ./security-sandbox-test.sh
#
# 原理：以论文表 4-9 与 DockerSandbox.buildHostConfig 完全一致的
#       docker run 限制参数运行每个恶意样例，断言容器被限/被杀，
#       并检查宿主机资源稳定（残留容器、内存、磁盘）。
# =====================================================================
set -u
cd "$(dirname "$0")"

IMG_C="${IMG_C:-codeoj/c:gcc13}"
IMG_CPP="${IMG_CPP:-codeoj/cpp:gcc13}"
IMG_JAVA="${IMG_JAVA:-codeoj/java:openjdk17}"
IMG_PY="${IMG_PY:-codeoj/python:3.12}"

RUN_MEM_MB=272            # C/C++：256 题目限制 + 16MB 运行时余量；Python 使用 320MB
COMPILE_MEM_MB=512        # 编译容器内存
RUN_TIMEOUT_MS=1000       # 判题时间限制
COMPILE_TIMEOUT_MS=10000  # 编译时间限制
HARD_TIMEOUT_S=20         # 宿主看护硬上限（容器硬上限 15s + 余量）

WORK_ROOT=$(mktemp -d /tmp/judge-sec-test.XXXXXX)
LOG_DIR="${EVIDENCE_DIR:-$WORK_ROOT/log}"
mkdir -p "$LOG_DIR"

FAIL=0
TOTAL=0
PASS=0

host_mem_used() { free -m | awk '/Mem:/{print $3}'; }
host_disk_used() { df -B1M "$WORK_ROOT" | awk 'NR==2{print $3}'; }
MEM_BEFORE=$(host_mem_used)
DISK_BEFORE=$(host_disk_used)

cleanup() {
  docker ps -aq --filter "name=judge-test-$$-" | xargs -r docker rm -f >/dev/null 2>&1
  rm -rf "$WORK_ROOT"
}
trap cleanup EXIT

# 通用限制参数：与论文表 4-9 / DockerSandbox 一一对应
common_limits() { # $1=memory_mb
  echo --network none \
       --memory "${1}m" --memory-swap "${1}m" \
       --cpus 1.0 --pids-limit 64 \
       --read-only --tmpfs /tmp:rw,size=64m,noexec,nosuid,nodev,mode=1777 \
       --tmpfs /run/codeoj:rw,size=40m,noexec,nosuid,nodev,mode=0700 \
       --cap-drop ALL --cap-add SETUID --cap-add SETGID --security-opt no-new-privileges \
       --ulimit nofile=64:64 --ulimit nproc=64:64 --ulimit fsize=16777216 \
       -u 0:0 -w /work
}

# 在受限容器内执行 runner 命令（外层 timeout 充当宿主看护）
run_container() { # $1=case_id $2=image $3=mem_mb $4=timeout_ms $5=outfile $6=errfile $7=compile/run 其余=命令
  local cid="judge-test-$$-$1-$RANDOM"
  local dir="$WORK_ROOT/work/$1"
  local mount_mode=ro
  [ "$7" != compile ] || mount_mode=rw
  mkdir -p "$dir"
  chmod 777 "$dir"
  timeout "$HARD_TIMEOUT_S" docker run --name "$cid" \
    $(common_limits "$3") \
    -v "$dir:/work:$mount_mode" \
    "$2" /judge/runner.sh run --timeout-ms "$4" --memory-mb 256 \
    --input /work/input.txt --user 1000:1000 -- "${@:8}" \
    >"$5" 2>"$6"
  local rc=$?
  OOM_=$(docker inspect -f '{{.State.OOMKilled}}' "$cid" 2>/dev/null || echo false)
  docker rm -f "$cid" >/dev/null 2>&1   # 宿主看护兜底：残留容器强删
  return $rc
}

# 解析 runner 统计协议行 → EXIT_ TIME_ MEM_ TRUNC_
# -a：按文本处理（防御输出流中混入 NUL 导致 grep 判定二进制）
parse_stat() { # $1=stderr 文件
  local line
  line=$(grep -ao '###CODEOJ_STAT###.*' "$1" | tail -n1)
  EXIT_=-999; TIME_=0; MEM_=0; TRUNC_=0
  if [ -n "$line" ]; then
    EXIT_=$(echo "$line" | sed -n 's/.*exit=\([-0-9]*\).*/\1/p')
    TIME_=$(echo "$line" | sed -n 's/.*time_ms=\([0-9]*\).*/\1/p')
    MEM_=$(echo "$line"  | sed -n 's/.*mem_kb=\([0-9]*\).*/\1/p')
    TRUNC_=$(echo "$line" | sed -n 's/.*trunc=\([0-9]*\).*/\1/p')
  fi
  [ -z "$EXIT_" ] && EXIT_=-999
  [ -z "$TIME_" ] && TIME_=0
  [ -z "$MEM_" ] && MEM_=0
  [ -z "$TRUNC_" ] && TRUNC_=0
}

record() { # $1=id $2=result $3=detail
  if [ "$2" = "PASS" ]; then
    PASS=$((PASS + 1))
    printf '  \033[32mPASS\033[0m  %-8s %s\n' "$1" "$3"
  else
    FAIL=$((FAIL + 1))
    printf '  \033[31mFAIL\033[0m  %-8s %s\n' "$1" "$3"
  fi
}

# 单样例执行：$1=id $2=样例目录 $3=lang $4=image $5=compile(""=无) $6=run $7=谓词 $8=描述
run_case() {
  TOTAL=$((TOTAL + 1))
  local id="$1" sdir="$2" lang="$3" img="$4" compile_cmd="$5" run_cmd="$6" pred="$7" desc="$8"
  local dir="$WORK_ROOT/work/$id"
  mkdir -p "$dir"
  chmod 777 "$dir"
  cp "malicious/$sdir/$lang"/* "$dir"/ 2>/dev/null

  local comp_out="$LOG_DIR/$id.compile.out" comp_err="$LOG_DIR/$id.compile.err"
  local run_out="$LOG_DIR/$id.run.out" run_err="$LOG_DIR/$id.run.err"
  local detail=""

  if [ -n "$compile_cmd" ]; then
    run_container "$id" "$img" "$COMPILE_MEM_MB" "$COMPILE_TIMEOUT_MS" "$comp_out" "$comp_err" compile $compile_cmd
    parse_stat "$comp_err"
    detail="编译 exit=$EXIT_ time=${TIME_}ms mem=${MEM_}KB"
    # S-10 编译期攻击：断言作用于编译阶段结果
    if [ "$id" = "s10" ]; then
      if eval "[ $pred ]"; then
        record "$id" "PASS" "$detail（$desc）"
      else
        record "$id" "FAIL" "$detail ｜ 期望: $pred（$desc）"
      fi
      return
    fi
    if [ "$EXIT_" -ne 0 ]; then
      record "$id" "FAIL" "$detail（编译失败，$desc）"
      return
    fi
  fi

  local run_mem="$RUN_MEM_MB"
  [ "$lang" != python3 ] || run_mem=320
  run_container "$id" "$img" "$run_mem" "$RUN_TIMEOUT_MS" "$run_out" "$run_err" run $run_cmd
  parse_stat "$run_err"
  detail="运行 exit=$EXIT_ wall=${TIME_}ms mem=${MEM_}KB trunc=$TRUNC_ oom=$OOM_"
  if eval "[ $pred ]"; then
    record "$id" "PASS" "$detail（$desc）"
  else
    record "$id" "FAIL" "$detail ｜ 期望: $pred（$desc）"
  fi
}

echo "=============================================================="
echo " 恶意样例沙箱直测（论文表 6-2）"
echo " 镜像: c=$IMG_C cpp=$IMG_CPP py=$IMG_PY"
echo " 运行限制: 内存 ${RUN_MEM_MB}m / 时间 ${RUN_TIMEOUT_MS}ms / 硬上限 ${HARD_TIMEOUT_S}s"
echo "=============================================================="

# id    样例目录       语言  镜像      编译命令                              运行命令        谓词                                                   描述
run_case s01  s01-fork-bomb    c  "$IMG_C"  "gcc -O2 -std=c17 main.c -o main" "./main"        '"$EXIT_" -eq 124'                                        "fork 炸弹：pids-limit 拦截后空转至超时(TLE)"
run_case s02  s02-dead-loop    c  "$IMG_C"  "gcc -O2 -std=c17 main.c -o main" "./main"        '"$EXIT_" -eq 124 -a "$TIME_" -ge 800 -a "$TIME_" -le 3000' "死循环：超时 SIGKILL(TLE)"
run_case s03  s03-memory-bomb  c  "$IMG_C"  "gcc -O2 -std=c17 main.c -o main" "./main"        '"$OOM_" = true -o "$MEM_" -gt 262144' "内存炸弹：cgroup OOM 或可信测量超限(MLE)"
run_case s03-py s03-memory-bomb python3 "$IMG_PY" ""                      "python3 main.py" '"$OOM_" = true -o \( "$EXIT_" -eq 1 -a "$MEM_" -gt 262144 \)' "内存炸弹(py)：OOM 或可测量的题目内存超限"
run_case s04  s04-disk-fill    c  "$IMG_C"  "gcc -O2 -std=c17 main.c -o main" "./main"        '"$EXIT_" -eq 2 -o "$EXIT_" -eq 153' "磁盘写入：只读工作目录拒绝，或文件上限阻断"
run_case s05  s05-network      c  "$IMG_C"  "gcc -O2 -std=c17 main.c -o main" "./main"        '"$EXIT_" -eq 0'                                          "网络访问：network none 全阻断(自报告)"
run_case s06  s06-host-info    c  "$IMG_C"  "gcc -O2 -std=c17 main.c -o main" "./main"        '"$EXIT_" -eq 0'                                          "读取宿主机：敏感路径不可读(自报告)"
run_case s07  s07-privilege    c  "$IMG_C"  "gcc -O2 -std=c17 main.c -o main" "./main"        '"$EXIT_" -eq 0'                                          "提权尝试：setuid(0) 被拒(自报告)"
run_case s08  s08-output-bomb  c  "$IMG_C"  "gcc -O2 -std=c17 main.c -o main" "./main"        '"$EXIT_" -eq 153 -o "$TRUNC_" -eq 1'                     "输出轰炸：16MB 输出限制"
run_case s10  s10-compile-bomb cpp "$IMG_CPP" "g++ -O2 -std=c++17 main.cpp -o main" "./main"  '"$OOM_" = true -o "$EXIT_" -eq 124 -o "$MEM_" -gt 524288' "编译期攻击：编译资源限制(CE)"

echo "--------------------------------------------------------------"
echo " 宿主机稳定性检查"
LEFT=$(docker ps -aq --filter "name=judge-test-$$-" | wc -l)
if docker info >/dev/null 2>&1; then DAEMON="OK"; else DAEMON="DOWN"; fi
MEM_AFTER=$(host_mem_used)
DISK_AFTER=$(host_disk_used)
MEM_DELTA=$((MEM_AFTER - MEM_BEFORE))
DISK_DELTA=$((DISK_AFTER - DISK_BEFORE))
echo "  残留容器: $LEFT（要求 0）"
echo "  Docker 引擎: $DAEMON"
echo "  内存变化: ${MEM_DELTA}MB（要求 < 512）"
echo "  磁盘变化: ${DISK_DELTA}MB（要求 < 200）"
[ "$LEFT" -eq 0 ] || FAIL=$((FAIL + 1))
[ "$DAEMON" = "OK" ] || FAIL=$((FAIL + 1))
[ "$MEM_DELTA" -lt 512 ] || FAIL=$((FAIL + 1))
[ "$DISK_DELTA" -lt 200 ] || FAIL=$((FAIL + 1))

echo "=============================================================="
echo " 总计: $TOTAL  通过: $PASS  失败: $FAIL"
echo "=============================================================="
exit $((FAIL > 255 ? 255 : FAIL))
