//  Copyright 2023 Dremio Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package cmd

import (
	"flag"
	"fmt"
	"log"
	"os"
	"strings"
	"time"

	"github.com/rsvihladremio/dremio-stress/pkg/conf"
	"github.com/rsvihladremio/dremio-stress/pkg/protocol"
	"github.com/rsvihladremio/dremio-stress/pkg/querygen"
	"github.com/rsvihladremio/dremio-stress/pkg/stress"
)

// ParseProtocol will convert a string to a AccessMethod type
func ParseProtocol(method string) (conf.Protocol, error) {
	switch strings.ToLower(method) {
	case "http":
		return conf.HTTP, nil
	case "odbc":
		return conf.ODBC, nil
	default:
		return -1, fmt.Errorf("method: %v is not supported only 'ODBC' and 'HTTP' methods are currently supported", method)
	}
}

// ParseArgs will attempt to read the args from flags passed to the cli. It also does validation of the arguments
func ParseArgs() (conf.Args, error) {
	maxConcurrency := flag.Int("max-concurrency", 4, "max number of concurrent queries")
	timeout := flag.Duration("timeout-seconds", 60*time.Second, "default timeout for queries")
	duration := flag.Duration("duration-seconds", 10*time.Minute, "duration of dremio-stress run")
	user := flag.String("user", "dremio", "user to use at login")
	password := flag.String("password", "dremio123", "password to use at login")
	url := flag.String("url", "http://localhost:9047", "http(s) URL used for '-protocol http' or a odbc connection string for '-protocol odbc'")
	protocolResult := flag.String("protocol", "http", "communication protocol to use for stress http or odbc are available")

	verbose := flag.Bool("v", false, "add more verbose output")
	skipSSL := flag.Bool("skip-ssl", false, "works with '-protocol http' when using https")
	jsonConfigPath := flag.String("conf", "stress.json", "location of the stress.json to define the stress job. If one is not provided a default stress job is used")

	//Set usage output
	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, `## stress.json examples:
   
### Two queries will end up at more or less at 50%% usage each
{
  "queries": [
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv LIMIT 50\"",
      "frequency": 5
    },
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv\" where \"DATE\" between ':start' and ':end'",
      "frequency": 5,
      "parameters: {
        "start": ["2018-02-04","2018-02-05"],
        "end": ["2018-02-14","2018-02-15"]
      }
    }
  ]
}

### Using queryGroups to preform several ops in order, schemops will be called roughly 10%% of the time
{
  "queryGroups": [
    {
      "name": "schemaops",
      "queries": [
        "drop table if exits samples.\"samples.dremio.com\".\"nyc-taxi-trips\"",
        "create table samples.\"samples.dremio.com\".\"nyc-taxi-trips\" STORE AS (type => 'iceberg') AS SELECT (\"a\",\"b\" FROM (values('a', 'b')) as t(\"a\",\"b\"))",
        "select * from  samples.\"samples.dremio.com\".\"nyc-taxi-trips\""
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
      "parameters" :{
          "start": ["2018-02-04", "2018-02-05"],
          "end": ["2018-02-14","2018-02-15"]
      }
    }
  ]
}

Usage with http: 
	dremio-stress -user dremio -password dremio123 -url http://localhost:9047 -conf ./stress.json
Usage with odbc (using new Arrow Flight driver): 
	dremio-stress -user dremio -password dremio123 -url "Driver={Arrow Flight SQL ODBC DSN};ConnectionType=Direct;AuthenticationType=Plain;"  -conf ./stress.json
`)

		flag.PrintDefaults()
	}
	flag.Parse()
	protocolMethod, err := ParseProtocol(*protocolResult)
	if err != nil {
		return conf.Args{}, err
	}
	return conf.Args{
		ProtocolArgs: conf.ProtocolArgs{
			User:     *user,
			Password: *password,
			URL:      *url,
			SkipSSL:  *skipSSL,
			Timeout:  *timeout,
		},
		StressArgs: conf.StressArgs{
			Duration:       *duration,
			MaxConcurrency: *maxConcurrency,
			JSONConfigPath: *jsonConfigPath,
		},
		Protocol: protocolMethod,
		Verbose:  *verbose,
	}, nil
}

// Execute is the entry point function after the args have been parsed.
func Execute(args conf.Args) error {
	var protocolEngine protocol.Engine
	var err error
	var selectedEngine string
	switch args.Protocol {
	case conf.HTTP:
		selectedEngine = "HTTP"
		protocolEngine, err = protocol.NewHTTPEngine(args.ProtocolArgs)
		if err != nil {
			return fmt.Errorf("unable to initialize HTTP protocol engine: %w", err)
		}
		break
	case conf.ODBC:
		selectedEngine = "ODBC"
		protocolEngine, err = protocol.NewODBCEngine(args.ProtocolArgs)
		if err != nil {
			return fmt.Errorf("unable to initialize ODBC protocol engine: %w", err)
		}
		break
	}
	defer func() {
		if err := protocolEngine.Close(); err != nil {
			log.Printf("WARN: unable to close protocol engine '%v' due to %v", selectedEngine, err)
		}
	}()

	jsonText, err := os.ReadFile(args.StressArgs.JSONConfigPath)
	if err != nil {
		return err
	}
	stressConf, err := conf.ParseStressJson(string(jsonText))
	if err != nil {
		return err
	}
	queryGen := querygen.NewStressConfQueryGenerator(stressConf)
	return stress.Run(args.Verbose, protocolEngine, queryGen, args.StressArgs)
}
