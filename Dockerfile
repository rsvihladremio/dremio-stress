FROM --platform=linux/amd64 debian:stable-slim

WORKDIR /app

COPY . .

RUN mkdir build &&  \
    cd build && \
    apt update -y && \
    apt upgrade -y && \ 
    apt install -y wget odbcinst unixodbc unixodbc-dev alien && \
    wget https://go.dev/dl/go1.21.1.linux-amd64.tar.gz && \
    tar -C /usr/local -xzf go1.21.1.linux-amd64.tar.gz && \
    ln -s /usr/local/go/bin/go /usr/local/bin/go && \
    wget https://download.dremio.com/arrow-flight-sql-odbc-driver/arrow-flight-sql-odbc-driver-LATEST.x86_64.rpm && \
    alien --scripts $(ls ./arrow-flight-sql-odbc-driver-*) && \
    apt install -y $(ls ./arrow-flight-sql-odbc-driver*.deb) && \
    rm $(ls ./arrow-flight-sql-odbc-driver-*) && \
    cd /usr/lib/x86_64-linux-gnu/ && \
    ln -s libodbcinst.so.2.0.0 libodbcinst.so.1 && \
    ln -s libodbc.so.2.0.0 libodbc.so.1 && \
    ldconfig && \
    cd /app && \
    apt remove -y wget && \
    rm -fr build && \
    go build -tags odbc -o /usr/bin/dremio-stress . && \
    apt clean all && \
    rm -rf /var/cache/yum && \
    rm -fr /app

ENTRYPOINT ["/usr/bin/dremio-stress"]
