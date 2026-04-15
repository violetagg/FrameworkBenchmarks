FROM maven:3.9.9-eclipse-temurin-24-noble as maven
WORKDIR /reactor-netty
COPY pom.xml pom.xml
COPY src src
RUN mvn compile assembly:single -q

FROM maven:3.9.9-eclipse-temurin-24-noble
WORKDIR /reactor-netty
COPY --from=maven /reactor-netty/target/app.jar app.jar
COPY run_reactor_netty.sh run_reactor_netty.sh

EXPOSE 8080

ENTRYPOINT "./run_reactor_netty.sh"
