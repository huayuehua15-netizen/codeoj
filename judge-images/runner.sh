#!/bin/bash
# root supervisor 保有可信统计文件；timeout 和程序都以无 root 权限的 UID:GID 执行。
# time_ms 是程序墙钟时间，cpu_ms 为附加诊断；容器创建/启动不计入题目时间。
set -uo pipefail
export LC_ALL=C
umask 077
TIMEOUT_MS=1000
INPUT=/work/input.txt
PROGRAM_USER=1000:1000
[ "${1:-}" = run ] && shift
while [ $# -gt 0 ]; do
  case "$1" in
    --timeout-ms) TIMEOUT_MS="$2"; shift 2 ;;
    --memory-mb) shift 2 ;;
    --input) INPUT="$2"; shift 2 ;;
    --user) PROGRAM_USER="$2"; shift 2 ;;
    --) shift; break ;;
    *) break ;;
  esac
done
[[ "$TIMEOUT_MS" =~ ^[1-9][0-9]*$ && "$PROGRAM_USER" =~ ^[1-9][0-9]*:[1-9][0-9]*$ ]] || exit 70
[ $# -gt 0 ] && [ "$(id -u)" = 0 ] || exit 70
printf -v TIMEOUT_S '%d.%03d' "$((TIMEOUT_MS / 1000))" "$((TIMEOUT_MS % 1000))"
[ -f "$INPUT" ] || INPUT=/dev/null
# 独立 root 0700 tmpfs；不在用户可写 /tmp 或 /work 中放置任何统计元数据。
TRUSTED=$(mktemp -d /run/codeoj/exec.XXXXXX) || exit 70
trap 'rm -rf -- "$TRUSTED"' EXIT
STAT_FILE="$TRUSTED/stat"
OUT_FILE="$TRUSTED/stdout"
ERR_FILE="$TRUSTED/stderr"
# setpriv 清除补充组并设置全部 uid/gid，no-new-privileges 防止 exec 后重新获得权限。
# 内层 timeout 同样降权，能够对同 UID 的程序发信号，无需给 supervisor CAP_KILL。
/usr/bin/time -f '%e %U %S %M' -o "$STAT_FILE" \
  setpriv --reuid="${PROGRAM_USER%:*}" --regid="${PROGRAM_USER#*:}" --clear-groups --no-new-privs \
  timeout -k 1s "$TIMEOUT_S" "$@" < "$INPUT" > "$OUT_FILE" 2> "$ERR_FILE"
EXIT_CODE=$?
[ -s "$STAT_FILE" ] || exit 70
read -r WALL_S U_S S_S MAX_RSS_KB < <(tail -n 1 "$STAT_FILE")
[[ "$WALL_S" =~ ^[0-9]+([.][0-9]+)?$ && "$U_S" =~ ^[0-9]+([.][0-9]+)?$ && "$S_S" =~ ^[0-9]+([.][0-9]+)?$ && "$MAX_RSS_KB" =~ ^[0-9]+$ ]] || exit 70
TIME_MS=$(awk -v w="$WALL_S" 'BEGIN { printf "%.0f", w * 1000 }')
CPU_MS=$(awk -v u="$U_S" -v s="$S_S" 'BEGIN { printf "%.0f", (u+s) * 1000 }')
TRUNC=0
OUT_SIZE=$(wc -c < "$OUT_FILE")
ERR_SIZE=$(wc -c < "$ERR_FILE")
[ "$OUT_SIZE" -gt 16777216 ] && TRUNC=1
[ "$ERR_SIZE" -gt 16777216 ] && TRUNC=1
head -c 16777216 "$OUT_FILE"
head -c 4096 "$ERR_FILE" >&2
printf '\n###CODEOJ_STAT### exit=%s time_ms=%s cpu_ms=%s mem_kb=%s trunc=%s\n' \
  "$EXIT_CODE" "$TIME_MS" "$CPU_MS" "$MAX_RSS_KB" "$TRUNC" >&2
exit 0
