# Usa una imagen base con Java (versión compatible con tu Spring Boot)
FROM eclipse-temurin:21-jdk-jammy

# Directorio de trabajo en el contenedor
WORKDIR /app

# Copia el JAR construido (ajusta el nombre si usas Maven/Gradle)
COPY target/suncalc-batch-prod.jar app.jar

# Puerto expuesto (el mismo que usa tu Spring Boot)
EXPOSE 8091

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]