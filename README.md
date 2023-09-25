# dremio-stress

Simple tool to stress Dremio via ODBC and REST interfaces

## Run via the REST interface

```bash
dremio-stress -user dremio -password dremio123 -url http://localhost:9047 -conf ./stress.json
```

## Run via ODBC 


### Run via Docker

```bash
docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress -protocol odbc -user dremio -password dremio123 -url "Driver={Arrow Flight SQL ODBC Driver};ConnectionType=Direct;AuthenticationType=Plain;Host=host.docker.internal;Port=32010;useEncryption=false"  -conf /mnt/stress.json
```

### Run ODBC Directly With a Binary

#### Dependencies on Windows

* Dremio ODBC Driver
* X86\_64 (or AMD64) processor

#### Dependencies on Mac and Linux

* UnixODBC
* Dremio ODBC Driver
* X86\_64 (or AMD64) processor
* Apple Silicon not supported use the Docker option


```bash
dremio-stress -protocol odbc -user dremio -password dremio123 -url  "Driver={Arrow Flight SQL ODBC Driver};ConnectionType=Direct;AuthenticationType=Plain;Host=localhost;Port=32010;useEncryption=false" -conf ./stress.json
```

## Example stress.json files

### Two queries will end up at more or less at 50% usage each

```json
{
  "queries": [
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv LIMIT 50\"",
      "frequency": 5
    },
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv\" where \"DATE\" between ':start' and ':end'",
      "frequency": 5,
      "parameters": {
        "start": ["2018-02-04","2018-02-05"],
        "end": ["2018-02-14","2018-02-15"]
      }
    }
  ]
}
```


### Using queryGroups to preform several ops in order, the "schemops" group  will be called roughly 10% of the time

```json
{
  "queryGroups": [
    {
      "name": "schemaops",
      "queries": [
        "drop table if exists samples.\"samples.dremio.com\".\"A\"",
        "create table samples.\"samples.dremio.com\".\"A\" STORE AS (type => 'iceberg') AS SELECT \"a\",\"b\" FROM (values('a', 'b')) as t(\"a\",\"b\")",
        "select * from  samples.\"samples.dremio.com\".\"A\""
      ]
    }
  ],
  "queries": [
    {
      "queryGroup": "schemaops",
      "frequency": 1
    },
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv\" where \"DATE\" between ':start' and ':end'",
      "frequency": 9,
      "parameters": {
          "start": ["2018-02-04", "2018-02-05"],
          "end": ["2018-02-14","2018-02-15"]
      }
    }
  ]
}
```


## Flags

```bash
  -conf string
    	location of the stress.json to define the stress job. If one is not provided a default stress job is used (default "stress.json")
  -duration duration
    	duration of dremio-stress run (default 10m0s)
  -max-concurrency int
    	max number of concurrent queries (default 4)
  -password string
    	password to use at login (default "dremio123")
  -protocol string
    	communication protocol to use for stress http or odbc are available (default "http")
  -skip-ssl
    	works with '-protocol http' when using https
  -timeout duration
    	default timeout for queries (default 1m0s)
  -url string
    	http(s) URL used for '-protocol http' or a odbc connection string for '-protocol odbc' (default "http://localhost:9047")
  -user string
    	user to use at login (default "dremio")
  -v	add more verbose output
```

## Running Tests

* Install docker or have it running
* ./script/test
