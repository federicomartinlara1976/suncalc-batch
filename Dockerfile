# Usa una imagen base con Java (versión compatible con tu Spring Boot)
FROM eclipse-temurin:21-jdk-jammy

# Directorio de trabajo en el contenedor
WORKDIR /app

# Instala dependencias del sistema, compila e instala los paquetes de Octave
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        octave \
        octave-dev \
        git \
        make \
        g++ \
        python3 \
        python3-pip \
        libmpfr-dev \
        libgmp-dev && \
    pip3 install --no-cache-dir sympy && \
    mkdir -p /tmp/build && cd /tmp/build && \
    git clone --depth 1 https://github.com/federicomartinlara1976/octave.git && \
    cd octave/paquetes/astronomia && \
    make -f Makefile-6.4.0 && \
    make install -f Makefile-6.4.0 && \
    cd .. && \
    octave --eval 'pkg install "symbolic-3.2.1.tar.gz"; pkg install "astronomia-1.2.0.tar.gz"' && \
    cd / && rm -rf /tmp/build && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copia el JAR construido (ajusta el nombre si usas Maven/Gradle)
COPY target/suncalc-batch-prod.jar app.jar

# Puerto expuesto (el mismo que usa tu Spring Boot)
EXPOSE 8091

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
