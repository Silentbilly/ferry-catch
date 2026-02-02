# build stage
FROM gradle:8-jdk21 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle bootJar --no-daemon

# runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
