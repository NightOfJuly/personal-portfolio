FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/portfolio-ai-assistant-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

