FROM maven:3.6.1-jdk-11-slim as maven
WORKDIR /reactor-netty
COPY pom.xml pom.xml
COPY src src
RUN mvn compile assembly:single -q

FROM openjdk:11.0.3-jdk-slim
WORKDIR /reactor-netty
COPY --from=maven /reactor-netty/target/reactor-netty-example-0.1-jar-with-dependencies.jar app.jar

EXPOSE 8080

CMD ["java", "-server", "-XX:+UseNUMA", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-Dio.netty.buffer.checkBounds=false", "-Dio.netty.buffer.checkAccessible=false", "-Dio.netty.iouring.iosqeAsyncThreshold=32000", "-Dreactor.netty.http.server.lastFlushWhenNoRead=true", "-jar", "app.jar"]
