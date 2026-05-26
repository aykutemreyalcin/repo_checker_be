FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests -B && \
    cp target/*.jar app.jar

FROM eclipse-temurin:17-jre
RUN apt-get update && \
    apt-get install -y --no-install-recommends git && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/app.jar ./app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
