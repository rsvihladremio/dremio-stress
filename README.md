# dremio-stress

Simple tool to stress Dremio via JDBC and REST interfaces written for Java 8 (but works with 17)

## Run via the REST interface

```bash
java -jar dremio-stress.jar -u dremio  -p dremio123 -l http://localhost:9047 ./stress.json
```

## Run via JDBC


### Run via Docker

```bash
docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress dremio-stress --protocol JDBC -l "jdbc:arrow-flight-sql://host.docker.internal:32010/?useEncryption=false&user=dremio&password=dremio123"  /mnt/stress.json
```

### Run JDBC Directly With a Binary

```bash
java -jar dremio-stress.jar --protocol JDBC "jdbc:arrow-flight-sql://localhost:32010/?useEncryption=false&user=dremio&password=dremio" ./stress.json
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
Usage: java -jar dremio-stress.jar [-sv] [-d=<durationSeconds>] [-l=<dremioUrl>] [-p=<dremioHttpPassword>] [--protocol=<protocol>] [-q=<maxQueriesInFlight>] [-t=<httpTimeoutSeconds>] [-u=<dremioHttpUser>]
 <jsonConfig> [COMMAND]
using a defined JSON run a series of queries against dremio using various approaches
      <jsonConfig>        The file to use for stress definitions
  -d, --duration-seconds=<durationSeconds>
                          duration in seconds to run stress
  -l, --url=<dremioUrl>   JDBC connection string or HTTP url to connect
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

## Running Tests

* Install docker or have it running
* JDK 8+
* ./script/test
