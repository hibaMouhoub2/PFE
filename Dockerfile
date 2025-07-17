
FROM openjdk:17-jdk-slim


WORKDIR /app


COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .


RUN chmod +x mvnw


RUN ./mvnw dependency:go-offline -B

# Copier le code source
COPY src ./src


RUN ./mvnw clean package -DskipTests -Dfile.encoding=UTF-8 -Dproject.build.sourceEncoding=UTF-8


EXPOSE 8080


CMD ["java", "-jar", "target/registration-login-demo-0.0.1-SNAPSHOT.jar"]