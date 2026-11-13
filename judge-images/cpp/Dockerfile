# codeoj/cpp:gcc13（论文表 5-2：g++ -O2 -std=c++17）
# 构建命令（在 judge-images 目录下执行）：docker build -f cpp/Dockerfile -t codeoj/cpp:gcc13 .
FROM gcc:13-bookworm

RUN apt-get update && apt-get install -y --no-install-recommends time bash \
    && rm -rf /var/lib/apt/lists/*

COPY runner.sh /judge/runner.sh
RUN chmod 755 /judge/runner.sh && mkdir -p /work && chmod 777 /work

WORKDIR /work
CMD ["bash"]
