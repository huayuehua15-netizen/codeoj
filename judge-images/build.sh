#!/bin/bash
# =====================================================================
# build.sh —— 判题镜像构建与验证（论文 5.2 表 5-2 / 附录部署步骤）
#
# 为什么需要这个脚本：
#   五个判题镜像不在 Docker Hub 上，也不可能随 git 仓库分发（体积数 GB，
#   且镜像层是构建产物）。任何新环境 clone 下来，判题链路能启动、能收 MQ、
#   能写数据库，唯独真实判题会失败。手工敲五条 docker build 容易漏、容易
#   在错误的目录执行、也无法验证构建结果是否可用。本脚本把"构建 + 自检"
#   固化成一条命令，让新机器与答辩现场可以确定性复现。
#
# 用法：
#   ./build.sh                 # 构建全部五个镜像
#   ./build.sh cpp java        # 只构建指定语言
#   ./build.sh --verify-only   # 只做自检，不构建
#   ./build.sh --no-verify     # 只构建，不自检
#   ./build.sh --no-cache      # 忽略构建缓存（Dockerfile 变更后排查用）
#
# 环境变量：
#   BASE_REGISTRY_MIRROR  官方基础镜像的拉取前缀。国内网络直连 Docker Hub 常超时，
#                         设为例如 registry.cn-hangzhou.aliyuncs.com 或自建镜像站。
#                         设为空串表示直连 Docker Hub（默认）。
#   APT_MIRROR            Debian/Ubuntu 系基础镜像内的 apt 源。默认不改；
#                         可设为 mirrors.aliyun.com / mirrors.tuna.tsinghua.edu.cn。
#   SKIP_DOCKER_CHECK=1   跳过 Docker 可用性预检（CI 中自有用例时使用）。
#
# 退出码：0 全部成功；1 任一镜像构建或自检失败。
# =====================================================================
set -uo pipefail
cd "$(dirname "$0")"

# Git Bash / MSYS2 会把形如 /work 的参数当成 Windows 路径改写，使
# `docker run -w /work` 变成 `-w C:/Program Files/Git/work` 而直接报错。
# 目标平台是 Linux，但在 Windows 上用 Docker Desktop + Git Bash 构建是常见做法，
# 这里统一关掉路径自动转换，保证两种环境行为一致。
if [ -n "${MSYSTEM:-}" ] || [ "${OSTYPE:-}" = "msys" ]; then
  export MSYS_NO_PATHCONV=1
  export MSYS2_ARG_CONV_EXCL="*"
fi

# ---------------------------------------------------------------------
# 镜像清单：语言 -> (Dockerfile 相对路径, 期望 tag)
# tag 必须与 oj-judge/.../core/strategy/*Strategy.java 的 image() 返回值一致，
# 否则判题时 Docker 会报 "No such image"，提交结果全部变成 status=8 系统错误。
# ---------------------------------------------------------------------
LANGS=(c cpp java python node)
declare -A DOCKERFILE=(
  [c]=c/Dockerfile
  [cpp]=cpp/Dockerfile
  [java]=java/Dockerfile
  [python]=python/Dockerfile
  [node]=node/Dockerfile
)
declare -A TAG=(
  [c]="codeoj/c:gcc13"
  [cpp]="codeoj/cpp:gcc13"
  [java]="codeoj/java:openjdk17"
  [python]="codeoj/python:3.12"
  [node]="codeoj/node:20"
)
# 自检用的探针程序：语言名 -> "源文件名|源码"
declare -A PROBE_FILE=(
  [c]="main.c" [cpp]="main.cpp" [java]="Main.java" [python]="main.py" [node]="main.js"
)
declare -A PROBE_SRC=(
  [c]='#include <stdio.h>
int main(void){int a,b;if(scanf("%d %d",&a,&b)!=2)return 1;printf("%d\n",a+b);return 0;}'
  [cpp]='#include <iostream>
int main(){int a,b;if(!(std::cin>>a>>b))return 1;std::cout<<(a+b)<<std::endl;return 0;}'
  [java]='import java.util.Scanner;
public class Main{public static void main(String[] args){Scanner s=new Scanner(System.in);
int a=s.nextInt(),b=s.nextInt();System.out.println(a+b);}}'
  [python]='a, b = map(int, input().split())
print(a + b)'
  [node]='const fs=require("fs");
const [a,b]=fs.readFileSync(0,"utf8").trim().split(/\s+/).map(Number);
console.log(a+b);'
)
# 自检用的编译命令（与各 Strategy.compileCommand() 等价的子集即可）
declare -A PROBE_COMPILE=(
  [c]="gcc -O2 -std=c17 main.c -o main"
  [cpp]="g++ -O2 -std=c++17 main.cpp -o main"
  [java]="javac -encoding UTF-8 Main.java"
  [python]="" [node]=""
)
declare -A PROBE_RUN=(
  [c]="./main" [cpp]="./main" [java]="java -Xss8m Main" [python]="python3 main.py" [node]="node main.js"
)

BUILD=1 VERIFY=1 NO_CACHE=""
SELECT=()
for arg in "$@"; do
  case "$arg" in
    --verify-only) BUILD=0 ;;
    --no-verify)   VERIFY=0 ;;
    --no-cache)    NO_CACHE="--no-cache" ;;
    -h|--help)     sed -n '2,30p' "$0"; exit 0 ;;
    -*)            echo "未知参数：$arg（--help 查看用法）" >&2; exit 2 ;;
    *)             SELECT+=("$arg") ;;
  esac
done
[ ${#SELECT[@]} -gt 0 ] && LANGS=("${SELECT[@]}")

for lang in "${LANGS[@]}"; do
  if [ -z "${TAG[$lang]:-}" ]; then
    echo "未知语言：$lang（可选：c cpp java python node）" >&2
    exit 2
  fi
done

# ---------------------------------------------------------------------
# 预检
# ---------------------------------------------------------------------
if [ "${SKIP_DOCKER_CHECK:-0}" != "1" ]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "错误：找不到 docker 命令。判题镜像必须在装有 Docker Engine 的机器上构建。" >&2
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    echo "错误：Docker 守护进程不可用（docker info 失败）。" >&2
    echo "      Linux：确认 dockerd 已启动且当前用户属于 docker 组。" >&2
    echo "      Windows/macOS：确认 Docker Desktop 正在运行。" >&2
    exit 1
  fi
fi

if [ ! -f runner.sh ]; then
  echo "错误：当前目录缺少 runner.sh，镜像内找不到 /judge/runner.sh 会直接判题失败。" >&2
  exit 1
fi
# runner.sh 被 COPY 进镜像。CRLF 会让 bash 报 "bad interpreter: /bin/bash^M"，
# 表现为所有提交都是系统错误。仓库 .gitattributes 已锁定 LF，这里再兜一层。
# 注意：不要用 grep -q $'\r' —— grep 会把 CR 当成二进制内容处理，不同版本行为不一致
# （实测 Git Bash 自带 GNU grep 3.0 直接不匹配，校验会静默失效）。tr 是二进制安全的。
if [ -n "$(tr -dc '\r' < runner.sh)" ]; then
  echo "错误：runner.sh 含有 CRLF 行尾。请执行 'dos2unix runner.sh' 或重设 core.autocrlf 后再构建。" >&2
  exit 1
fi
for lang in "${LANGS[@]}"; do
  if [ ! -f "${DOCKERFILE[$lang]}" ]; then
    echo "错误：找不到 ${DOCKERFILE[$lang]}" >&2
    exit 1
  fi
done

# 可选：改写基础镜像前缀与 apt 源。这里用临时 dockerfile 覆盖，
# 不修改仓库里的 Dockerfile，避免把个人网络环境写进版本库。
# 临时目录放在脚本同级（相对路径），这样 docker build -f 在 Windows/Git Bash
# 下也能正确解析；退出时统一清理。
WORK=$(mktemp -d "./.build-tmp.XXXXXX") || { echo "错误：无法创建临时目录" >&2; exit 1; }
trap 'rm -rf -- "$WORK"' EXIT

# 把宿主机路径转成 Docker 守护进程能理解的绝对路径。
# Git Bash 下 mktemp 给的是 /tmp/... 这类 POSIX 路径，Windows 上的 dockerd 不认，
# 需要 cygpath 换成 C:\... 形式；Linux 上原样返回。
host_path() {
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -w "$(cd "$1" && pwd)"
  else
    (cd "$1" && pwd)
  fi
}

prepare_dockerfile() { # $1=lang -> 打印要用的 dockerfile 路径
  local src="${DOCKERFILE[$1]}" out="$WORK/$1.Dockerfile"
  cp "$src" "$out"
  if [ -n "${BASE_REGISTRY_MIRROR:-}" ]; then
    # 只改写 FROM 行里没有仓库域名的官方镜像（gcc:13-bookworm -> <mirror>/gcc:13-bookworm）
    sed -i.bak -E "s|^FROM ([^/]+)$|FROM ${BASE_REGISTRY_MIRROR%/}/\1|" "$out"
  fi
  if [ -n "${APT_MIRROR:-}" ]; then
    # 在首次 apt-get update 之前插入 sed 改写 sources.list
    sed -i.bak "/apt-get update/i\\
RUN sed -i \"s|deb.debian.org|${APT_MIRROR}|g; s|security.debian.org|${APT_MIRROR}|g; s|archive.ubuntu.com|${APT_MIRROR}|g; s|security.ubuntu.com|${APT_MIRROR}|g\" /etc/apt/sources.list /etc/apt/sources.list.d/* 2>/dev/null || true" "$out"
  fi
  rm -f "$out.bak"
  echo "$out"
}

build_one() { # $1=lang
  local lang="$1" tag="${TAG[$1]}" df
  df=$(prepare_dockerfile "$lang")
  printf '\n\033[36m>>> 构建 %s（%s）\033[0m\n' "$tag" "${DOCKERFILE[$lang]}"
  # 构建上下文是 judge-images 目录本身，因为 runner.sh 在根上、被各 Dockerfile COPY
  if docker build $NO_CACHE -f "$df" -t "$tag" .; then
    printf '\033[32m    构建成功\033[0m\n'
    return 0
  fi
  printf '\033[31m    构建失败：%s\033[0m\n' "$tag"
  echo "    排查建议：" >&2
  echo "      1) 网络：设置 BASE_REGISTRY_MIRROR=<镜像站> 后重试；" >&2
  echo "      2) 磁盘：docker system df 查看空间，必要时 docker builder prune；" >&2
  echo "      3) 上下文：必须在 judge-images/ 目录执行（脚本已自动 cd）。" >&2
  return 1
}

# ---------------------------------------------------------------------
# 自检：在镜像内跑一次真实编译+运行+统计协议，验证镜像可用而非仅"构建成功"。
# 只验证 runner.sh 与工具链共存，不验证沙箱限制（那是 judge-tests 的职责）。
# ---------------------------------------------------------------------
verify_one() { # $1=lang
  local lang="$1" tag="${TAG[$1]}" src="${PROBE_SRC[$1]}" file="${PROBE_FILE[$1]}"
  local compile_cmd="${PROBE_COMPILE[$1]}" run_cmd="${PROBE_RUN[$1]}"
  local dir; dir=$(mktemp -d "$WORK/verify-$lang.XXXXXX")
  printf '%s' "$src" > "$dir/$file"
  printf '3 4\n' > "$dir/input.txt"
  chmod -R 777 "$dir"

  local script="set -e
cd /work
"
  if [ -n "$compile_cmd" ]; then script+="$compile_cmd
"; fi
  script+="/judge/runner.sh run --timeout-ms 5000 --memory-mb 256 --input /work/input.txt --user 1000:1000 -- $run_cmd"

  local mnt; mnt=$(host_path "$dir")

  # 与 DockerSandbox.buildHostConfig 保持同构的最小限制集，确保 runner.sh 在
  # 真实的降权 + 只读根 + tmpfs 组合下也能工作（而不是只在宽松容器里能跑）。
  local out err rc
  out=$(timeout 120 docker run --rm \
      --network none --memory 512m --memory-swap 512m --cpus 1.0 --pids-limit 64 \
      --read-only --tmpfs /tmp:rw,size=64m,noexec,nosuid,nodev,mode=1777 \
      --tmpfs /run/codeoj:rw,size=40m,noexec,nosuid,nodev,mode=0700 \
      --cap-drop ALL --cap-add SETUID --cap-add SETGID --security-opt no-new-privileges \
      --ulimit nofile=64:64 --ulimit nproc=64:64 --ulimit fsize=16777216 \
      -u 0:0 -w /work -v "$mnt:/work:rw" \
      "$tag" bash -c "$script" 2>"$dir/.stderr") ; rc=$?
  err=$(cat "$dir/.stderr" 2>/dev/null)

  local stat_line expect
  stat_line=$(printf '%s\n%s' "$out" "$err" | grep -ao '###CODEOJ_STAT###.*' | tail -n1)
  if [ $rc -ne 0 ]; then
    printf '\033[31m    自检失败 %s：容器退出码 %s\033[0m\n' "$tag" "$rc"
    printf '    stderr：%s\n' "$(printf '%s' "$err" | head -c 500)" >&2
    return 1
  fi
  if [ -z "$stat_line" ]; then
    printf '\033[31m    自检失败 %s：未输出统计协议行（runner.sh 未正常执行）\033[0m\n' "$tag"
    printf '    stdout：%s\n' "$(printf '%s' "$out" | head -c 500)" >&2
    return 1
  fi
  # 探针是 3+4，正确答案为 7；同时要求 exit=0
  expect=$(printf '%s' "$out" | tr -d '\r')
  if [ "$expect" != "7" ]; then
    printf '\033[31m    自检失败 %s：期望输出 7，实际 %s\033[0m\n' "$tag" "$(printf '%s' "$expect" | head -c 200)"
    return 1
  fi
  if ! printf '%s' "$stat_line" | grep -q 'exit=0'; then
    printf '\033[31m    自检失败 %s：统计行未报告 exit=0（%s）\033[0m\n' "$tag" "$stat_line"
    return 1
  fi
  printf '\033[32m    自检通过（输出 7，%s）\033[0m\n' "$(printf '%s' "$stat_line" | sed 's/###CODEOJ_STAT### //')"
  return 0
}

# ---------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------
FAILED=()
printf '判题镜像构建：%s\n' "${LANGS[*]}"
[ -n "${BASE_REGISTRY_MIRROR:-}" ] && printf '基础镜像镜像站：%s\n' "$BASE_REGISTRY_MIRROR"
[ -n "${APT_MIRROR:-}" ] && printf 'apt 源：%s\n' "$APT_MIRROR"

for lang in "${LANGS[@]}"; do
  if [ "$BUILD" = "1" ]; then
    build_one "$lang" || { FAILED+=("$lang(构建)"); continue; }
  fi
  if [ "$VERIFY" = "1" ]; then
    printf '\033[36m>>> 自检 %s\033[0m\n' "${TAG[$lang]}"
    verify_one "$lang" || FAILED+=("$lang(自检)")
  fi
done

printf '\n=====================================================================\n'
if [ ${#FAILED[@]} -eq 0 ]; then
  printf '\033[32m全部完成：%d 个镜像可用。\033[0m\n' "${#LANGS[@]}"
  printf '下一步：启动 oj-judge（ENGINE_MODE=real）后提交一道题验证端到端判定。\n'
  exit 0
fi
printf '\033[31m失败项：%s\033[0m\n' "${FAILED[*]}"
printf '修复后可用 ./build.sh %s 单独重跑。\n' "${FAILED[*]%%(*}"
exit 1
