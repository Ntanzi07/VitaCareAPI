FROM gradle:8.6-jdk21 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
# Ensure the Gradle wrapper is executable and use it to build (keeps Gradle version pinned)
RUN chmod +x ./gradlew && ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
# copy the built jar from the build stage to root so ENTRYPOINT can find /app.jar
COPY --from=build /home/gradle/project/build/libs/*.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
