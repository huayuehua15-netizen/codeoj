# codeoj/java:openjdk17（论文表 5-2：javac -encoding UTF-8 Main.java；java -Xss8m -Xmx{mem}m Main）
# 构建命令（在 judge-images 目录下执行）：docker build -f java/Dockerfile -t codeoj/java:openjdk17 .
FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y --no-install-recommends time bash \
    && rm -rf /var/lib/apt/lists/*

COPY runner.sh /judge/runner.sh
RUN chmod 755 /judge/runner.sh && mkdir -p /work && chmod 777 /work

WORKDIR /work
CMD ["bash"]
