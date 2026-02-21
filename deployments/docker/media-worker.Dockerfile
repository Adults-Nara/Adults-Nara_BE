FROM eclipse-temurin:21-jre
WORKDIR /app
COPY apps/media-worker/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
