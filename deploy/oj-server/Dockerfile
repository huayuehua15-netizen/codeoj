# =====================================================================
# oj-server 多阶段构建（论文 5.8）：仅保留 JRE 运行层
# =====================================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY oj-common/pom.xml oj-common/pom.xml
COPY oj-server/pom.xml oj-server/pom.xml
COPY oj-judge/pom.xml oj-judge/pom.xml
RUN mvn -q dependency:go-offline -pl oj-common,oj-server -am || true

COPY oj-common/src oj-common/src
COPY oj-server/src oj-server/src
RUN mvn -q package -DskipTests -pl oj-common,oj-server -am

FROM eclipse-temurin:17-jre
COPY --from=build /app/oj-server/target/oj-server.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
