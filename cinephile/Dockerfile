# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q clean package -DskipTests
RUN mv target/*.jar target/app.jar

# Runtime stage
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=builder /build/target/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]