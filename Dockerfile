FROM gradle:8.12-jdk23 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN chmod +x ./gradlew && ./gradlew build -x test --no-daemon

FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]