FROM eclipse-temurin:21-jre

RUN apt-get update \
 && apt-get install -y --no-install-recommends ffmpeg \
 && rm -rf /var/lib/apt/lists/* \
 && ffmpeg -version

WORKDIR /app
COPY apps/media-worker/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]