FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/target/nexus-bank-enterprise-1.0.0.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
