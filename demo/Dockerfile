# 构建阶段
FROM maven:3.8.6-eclipse-temurin-17 AS builder

WORKDIR /app

# 先复制 pom.xml 并利用 Docker 缓存依赖层
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并打包
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre

WORKDIR /app

# 从构建阶段复制生成的 jar
COPY --from=builder /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# 通过 sh -c 引用 JAVA_OPTS，使 K8s 中设置的 JVM 参数生效
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
