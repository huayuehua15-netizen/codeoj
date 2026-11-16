# codeoj/node:20（前端方向题目：JavaScript / Vue3 逻辑题，node main.js）
# 构建命令（在 judge-images 目录下执行）：docker build -f node/Dockerfile -t codeoj/node:20 .
FROM node:20-bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends time bash coreutils util-linux \
    && rm -rf /var/lib/apt/lists/*

COPY runner.sh /judge/runner.sh
RUN chmod 755 /judge/runner.sh && mkdir -p /work && chmod 777 /work

WORKDIR /work
CMD ["node"]
