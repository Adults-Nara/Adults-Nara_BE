FROM eclipse-temurin:21-jre
WORKDIR /app
COPY apps/core-api/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
