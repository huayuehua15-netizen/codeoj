# codeoj/python:3.12（论文表 5-2：python3 main.py，无编译）
# 构建命令（在 judge-images 目录下执行）：docker build -f python/Dockerfile -t codeoj/python:3.12 .
FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends time bash \
    && rm -rf /var/lib/apt/lists/*

COPY runner.sh /judge/runner.sh
RUN chmod 755 /judge/runner.sh && mkdir -p /work && chmod 777 /work

WORKDIR /work
CMD ["bash"]
