# dremio-stress

Simple tool to stress Dremio via JDBC and REST interfaces written for Java 17 against a queries.json or using a custom workload file (stress.json).

## Run via the REST interface

```bash
docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress:0.4.0 dremio-stress -g STRESS_JSON -u dremio -p dremio123 -l http://host.docker.internal:9047 /mnt/stress.json
```

## Run via JDBC


### Run via Docker

```bash
docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress:0.4.0 dremio-stress -g QUERIES_JSON --protocol JDBC -l "jdbc:arrow-flight-sql://host.docker.internal:32010/?useEncryption=false&user=dremio&password=dremio123"  /mnt/queries.json
```

### JDBC with Docker

```bash
docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress:0.4.0 dremio-stress -g QUERIES_JSON --protocol JDBC -l "jdbc:arrow-flight-sql://host.docker.internal:32010/?useEncryption=false&user=dremio&password=dremio" /mnt/queries.json
```

### Using custom stress.json format with specified workloads

```bash
docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress:0.4.0 dremio-stress -g STRESS_JSON --protocol JDBC -l "jdbc:arrow-flight-sql://host.docker.internal:32010/?useEncryption=false&user=dremio&password=dremio" /mnt/stress.json
```

## Run via Legacy JDBC 


### Run via Docker

```bash
docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress:0.4.0 dremio-stress -g QUERIES_JSON --protocol LegacyJDBC -l "jdbc:dremio:direct=host.docker.internal:31010;user=dremio;password=dremio123"  /mnt/queries.json
```
## Example stress.json files

### Using queryGroups to preform several ops in order

NOTE: the "schema-ops" group  will be called roughly 10% of the time

```json
{
  "queryGroups": [
	{
	  "name": "schema-ops",
	  "queries": [
        "drop table if exists samples.\"samples.dremio.com\".\"A\"",
		"create table samples.\"samples.dremio.com\".\"A\" STORE AS (type => 'iceberg') AS SELECT \"a\",\"b\" FROM (values('a', 'b')) as t(\"a\",\"b\")",
		"select * from  samples.\"samples.dremio.com\".\"A\""
	  ]
    }
  ],
  "queries": [
    {
      "queryGroup": "schema-ops",
      "frequency": 1
    },
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv\" where \"DATE\" between ':start' and ':end' and :seq = passenger_count",
      "frequency": 9,
      "parameters": {
        "start": [
          "2018-02-04",
          "2018-02-05"
        ],
        "end": [
          "2018-02-14",
          "2018-02-15"
        ]
      },
      "sequence": {
        "name": "seq",
        "start": 1,
        "end": 20,
        "step": 1
      }
    }
  ]
}
```


## Flags

```bash
Usage: dremio-stress [-sv] [-d=<durationSeconds>] [-g=<queriesGeneratorFileType>] [-l=<dremioUrl>] [--limit-results=<limitResults>] [-p=<dremioHttpPassword>] [--protocol=<protocol>] [-q=<max
QueriesInFlight>] [-t=<httpTimeoutSeconds>] [-u=<dremioHttpUser>] <jsonConfig> [COMMAND]
using a defined JSON run a series of queries against dremio using various approaches
      <jsonConfig>        The file to use for query definitions. Supports queries.json.gz, queries.json, or a directory of queries.json and a stress.json file with a defined workload (see example)
  -d, --duration-seconds=<durationSeconds>
                          duration in seconds to run stress
  -g, --generator-type=<queriesGeneratorFileType>
                          specify QUERIES_JSON or STRESS_JSON to specify the engine type
  -l, --url=<dremioUrl>   JDBC connection string or HTTP url to connect
      --limit-results=<limitResults>
                          limit results to the specified number assuming there is not already a LIMIT in the query. This is an easy way to just add some limits on the result set size
  -p, --http-password=<dremioHttpPassword>
                          the password of the user used to submit HTTP queries
      --protocol=<protocol>
                          protocol to use HTTP or JDBC
  -q, --max-queries-in-flight=<maxQueriesInFlight>
                          max number of queries in flight (if possible)
  -s, --http-skip-ssl-verification
                          whether to skip ssl verification for HTTP queries or not
  -t, --http-timeout-seconds=<httpTimeoutSeconds>
                          HTTP timeout for queries
  -u, --http-user=<dremioHttpUser>
                          the user used to submit HTTP queries
  -v, --verbose           -v for info, -vv for debug, -vvv for trace
```

## Contributing 

* `git clone git@github.com:rsvihladremio/dremio-stress`
* cd dremio-stress

### How to build the Docker image

* Install docker desktop or colima or whatever you desire that runs works with a Dockerfile
* run `docker build -t ghcr.io/rsvihladremio/dremio-stress .`
* run `docker run -it ghcr.io/rsvihladremio/dremio-stress dremio-stress` and you should see the help output

### Running Tests

* Install docker or have it running
* JDK 17
* run `./script/test`

### Cutting a new release

This assumes the [github client is installed and configured](https://cli.github.com) and assuming release version 0.4.0. Update the version to your desired version.


* run in a terminal `export VERSION=0.4.0`
* run `./mvnw versions:set versions:commit -DnewVersion="$VERSION"`
* run `git tag v$VERSION`
* run `git push origin v$VERSION`
* run `./script/build`
* run `gh release create v$VERSION --generate-notes ./target/dremio-stress.jar`

