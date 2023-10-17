FROM ghcr.io/graalvm/native-image-community:17

RUN mkdir -p /app
WORKDIR /app
ARG VERSION=unknown
ARG GIT_SHA=unknown
COPY . /app

RUN ./mvnw package -Pnative -DskipTests=true

FROM debian:12-slim
WORKDIR /app/
COPY --from=0 /app/target/dremio-stress /usr/bin/dremio-stress

ENTRYPOINT ["/usr/bin/dremio-stress"]
