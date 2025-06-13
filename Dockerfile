# Utiliser une image de base avec Java 17
FROM openjdk:17-jdk-slim

# Définir le répertoire de travail dans le container
WORKDIR /app

# Copier le fichier pom.xml et le wrapper Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Donner les permissions d'exécution au wrapper Maven
RUN chmod +x mvnw

# Télécharger les dépendances (cette étape sera mise en cache si pom.xml ne change pas)
RUN ./mvnw dependency:go-offline -B

# Copier le code source
COPY src ./src

# Construire l'application avec encodage UTF-8
RUN ./mvnw clean package -DskipTests -Dfile.encoding=UTF-8 -Dproject.build.sourceEncoding=UTF-8

# Exposer le port 8080
EXPOSE 8080

# Définir la commande pour démarrer l'application
CMD ["java", "-jar", "target/registration-login-demo-0.0.1-SNAPSHOT.jar"]