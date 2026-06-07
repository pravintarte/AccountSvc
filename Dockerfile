FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/account-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
