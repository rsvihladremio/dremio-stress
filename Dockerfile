FROM eclipse-temurin:17

RUN mkdir -p /app
WORKDIR /app
COPY . /app

RUN ./mvnw validate && ./mvnw package -Pnative -DskipTests=true && echo "#!/usr/bin/env bash\n_JAVA_OPTIONS='--add-opens=java.base/java.nio=ALL-UNNAMED' java -jar $(ls ./target/dremio-stress-*-jar-with-dependencies.jar) \"\$@\"" >> /usr/bin/dremio-stress && chmod +x /usr/bin/dremio-stress

CMD ["bash", "/usr/bin/dremio-stress"]
