FROM openjdk:8-jdk-alpine
ARG JAR_FILE=application/target/*.jar

COPY . db-server
WORKDIR db-server

CMD ./mvnw clean; ./mvnw install; ./mvnw install -f application/pom.xml; ./mvnw spring-boot:run; ./mvnw spring-boot:run -X -f application/pom.xml
