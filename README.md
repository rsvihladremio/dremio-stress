# dremio-stress

Simple tool to stress Dremio via ODBC and REST interfaces

## Run via the REST interface

```bash
dremio-stress -user dremio -password dremio123 -l http://localhost:9047 ./stress.json
```

## Run via JDBC 


### Run via Docker

```bash
docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress --protocol jdbc -u dremio -p dremio123 -l "jdbc:arrow-flight-sql://host.docker.internal:32010/?useEncryption=false"  /mnt/stress.json
```

### Run JDBC Directly With a Binary

```bash
dremio-stress --protocol jdbc -u dremio -p dremio123 -l  "jdbc:arrow-flight-sql://localhost:32010/?useEncryption=false;" ./stress.json
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


### Using queryGroups to preform several ops in order, the "schemaops" group  will be called roughly 10% of the time

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
Usage: dremio-stress [-s] [-d=<durationSeconds>] [-l=<dremioHost>] [-p=<dremioPassword>] [-q=<maxQueriesInFlight>] [-t=<timeoutSeconds>] [-u=<dremioUser>] <yamlConfig> [COMMAND]                                
using a defined JSON run a series of queries against dremio using various approaches                                                                                                                      
      <yamlConfig>          The file to use for stress definitions                                                                                                                                        
  -d, --duration-seconds=<durationSeconds>                                                                                                                                                                
                            duration in seconds to run stress                                                                                                                                             
  -l, --host=<dremioHost>   the http url of the dremio server which is used to submit sql and create spaces
  -p, --password=<dremioPassword>
                            the password of the user used to submit sql and create spaces to the rest api
  -q, --max-queries-in-flight=<maxQueriesInFlight>
                            max number of queries in flight (if possible)
  -s, --skip-ssl-verification
                            whether to skip ssl verification for queries or not
  -t, --timeout-seconds=<timeoutSeconds>
                            timeout for queries
  -u, --user=<dremioUser>   the user used to submit sql and create spaces to the rest api
```

## Running Tests

* Install docker or have it running
* ./script/test
