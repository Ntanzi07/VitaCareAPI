FROM gradle:8.6-jdk21 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN chmod +x ./gradlew && ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
# Exclude the plain jar, copy only the fat jar
COPY --from=build /home/gradle/project/build/libs/*[^plain].jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]