FROM --platform=linux/amd64 fedora:38

WORKDIR /app

RUN mkdir build &&  \
    cd build && \
    yum install -y wget unixODBC unixODBC-devel go && \
    wget https://download.dremio.com/arrow-flight-sql-odbc-driver/arrow-flight-sql-odbc-driver-LATEST.x86_64.rpm && \
    yum install -y $(ls ./arrow-flight-sql-odbc-driver-*) && \
    rm $(ls ./arrow-flight-sql-odbc-driver-*) && \
    cd /usr/lib64/ && \
    ln -s libodbcinst.so.2.0.0 libodbcinst.so.1 && \
    ln -s libodbc.so.2.0.0 libodbc.so.1 && \
    ldconfig && \
    cd /app && \
    yum remove -y wget && \
    rm -fr build

COPY . .

RUN go build -o /usr/bin/dremio-stress . && yum remove -y wget go && rm -fr /app && yum clean all && rm -rf /var/cache/yum

ENTRYPOINT ["/usr/bin/dremio-stress"]
