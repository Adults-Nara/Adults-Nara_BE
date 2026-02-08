FROM eclipse-temurin:21-jre
WORKDIR /app
COPY apps/batch-analytics/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
