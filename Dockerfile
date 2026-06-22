FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine AS server

RUN apk add --no-cache wget && \
    addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=builder /app/target/ledger-*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]


FROM eclipse-temurin:21-jdk-alpine AS bench

WORKDIR /bench

COPY benchmark/LoadTest.java .
RUN javac LoadTest.java

ENTRYPOINT ["java", "LoadTest"]