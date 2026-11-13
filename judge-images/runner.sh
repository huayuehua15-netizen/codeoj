#!/bin/bash
# =====================================================================
# runner.sh —— 沙箱容器内执行器（论文 5.4）
# 由判题服务以参数列表调用（不经 shell 拼接用户参数）：
#   /judge/runner.sh run --timeout-ms <ms> --memory-mb <mb> --input <file> <cmd...>
#
# 职责：
#   1. 以 /usr/bin/time 记录子进程 CPU 时间与峰值内存；
#   2. 超时控制：timeout 先发 SIGTERM，-k 1 秒后 SIGKILL（容器内第一重超时；
#      宿主侧看护定时器为第二重，论文 5.4 双重超时控制）；
#   3. stdin 来自 --input 文件，stdout/stderr 原样输出；
#   4. 输出超过 16MB 截断，防止比对阶段内存溢出（论文 5.4）。
# =====================================================================
set -u

TIMEOUT_MS=1000
INPUT=/work/input.txt
CMD=()

while [ $# -gt 0 ]; do
  case "$1" in
    --timeout-ms) TIMEOUT_MS="$2"; shift 2 ;;
    --memory-mb)  shift 2 ;;                      # 内存由 cgroup 强制，runner 仅透传
    --input)      INPUT="$2"; shift 2 ;;
    *)            CMD+=("$1"); shift ;;
  esac
done

TIMEOUT_S="$((TIMEOUT_MS / 1000)).$((TIMEOUT_MS % 1000))"

STAT_FILE=$(mktemp /tmp/stat.XXXXXX)
OUT_FILE=$(mktemp /tmp/out.XXXXXX)

# /usr/bin/time -f "%e %M %x"：墙钟秒数 / 峰值内存 KB / 退出码
/usr/bin/time -f "%e %M %x" -o "$STAT_FILE" \
  timeout -k 1s "$TIMEOUT_S" "${CMD[@]}" < "$INPUT" > "$OUT_FILE" 2>&1
EXIT_CODE=$?

# 输出 16MB 截断（论文 5.4：输出超过 16MB 判运行错误）
truncate -s 16M "$OUT_FILE" 2>/dev/null || true

# 读取统计行
ELAPSED_S="0"; MAX_RSS_KB="0"
if [ -s "$STAT_FILE" ]; then
  LAST_LINE=$(tail -n 1 "$STAT_FILE")
  ELAPSED_S=$(echo "$LAST_LINE" | awk '{print $1}')
  MAX_RSS_KB=$(echo "$LAST_LINE" | awk '{print $2}')
fi

TIME_MS=$(awk -v s="$ELAPSED_S" 'BEGIN { printf "%d", s * 1000 }')

# stdout：程序输出
cat "$OUT_FILE"

# stderr：统计协议行（宿主解析）
echo "###CODEOJ_STAT### exit=$EXIT_CODE time_ms=$TIME_MS mem_kb=$MAX_RSS_KB" >&2

rm -f "$STAT_FILE" "$OUT_FILE"
exit 0
