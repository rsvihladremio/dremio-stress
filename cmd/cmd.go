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

	"github.com/rsvihladremio/dremio-stress/pkg/args"
	"github.com/rsvihladremio/dremio-stress/pkg/conf"
	"github.com/rsvihladremio/dremio-stress/pkg/gen"
	"github.com/rsvihladremio/dremio-stress/pkg/protocol"
	"github.com/rsvihladremio/dremio-stress/pkg/stress"
)

// FileReader is an interface that declares the ReadFile method. It's used to abstract away the actual
// mechanism for reading a file, so that different implementations can be swapped in without changing the code
// that uses this interface. This is particularly useful in testing, where we can replace actual file operations
// with a mock.
type FileReader interface {
	ReadFile(filename string) ([]byte, error)
}

// OsFileReader is the real-world implementation of the FileReader interface. This uses os.ReadFile
// method from the Go's standard library to read the contents of a file. It's used when the application runs
// in the production environment.
type OsFileReader struct{}

// Using the os.ReadFile function from the Go standard library to read a file. This function returns
// the contents of the file as a byte slice and an error. If the file read operation was successful,
// the error will be nil.
func (o OsFileReader) ReadFile(filename string) ([]byte, error) {
	return os.ReadFile(filename)
}

// ParseArgs will attempt to read the args from flags passed to the cli. It also does validation of the arguments
func ParseArgs() (args.Args, error) {
	maxConcurrency := flag.Int("max-concurrency", 4, "max number of concurrent queries")
	timeout := flag.Duration("timeout", 60*time.Second, "default timeout for queries")
	duration := flag.Duration("duration", 10*time.Minute, "duration of dremio-stress run")
	user := flag.String("user", "dremio", "user to use at login")
	password := flag.String("password", "dremio123", "password to use at login")
	url := flag.String("url", "http://localhost:9047", "http(s) URL used for '-protocol http' or a odbc connection string for '-protocol odbc'")
	protocolMethod := flag.String("protocol", "http", "communication protocol to use for stress http or odbc are available")

	verbose := flag.Bool("v", false, "add more verbose output")
	skipSSL := flag.Bool("skip-ssl", false, "works with '-protocol http' when using https")
	jsonConfigPath := flag.String("conf", "stress.json", "location of the stress.json to define the stress job. If one is not provided a default stress job is used")

	//Set usage output
	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, `dremio-stress %s %s-%s

EXAMPLE stress.json:
  
    ## Using queryGroups to preform several ops in order, "schema" will be called roughly 10%% of the time.

    {
      "queryGroups": [
        {
          "name": "schema",
          "queries": [
            "drop table if exists samples.\"samples.dremio.com\".\"A\"",
            "create table samples.\"samples.dremio.com\".\"A\" STORE AS (type => 'iceberg') AS SELECT \"a\",\"b\" FROM (values('a', 'b')) as t(\"a\",\"b\")",
            "select * from  samples.\"samples.dremio.com\".\"A\""
          ]
        }
      ],
      "queries": [
        {
          "queryGroup": "schema",
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

USAGE: 
`, odbcDisabled, Version, GitSha)
		for k, v := range examples {
			fmt.Fprintf(os.Stderr, "%s:\n\n    %s\n\n", k, v)
		}
		fmt.Fprint(os.Stderr, "flags:\n\n")
		flag.PrintDefaults()
	}
	flag.Parse()
	return args.Args{
		ProtocolArgs: args.ProtocolArgs{
			User:     *user,
			Password: *password,
			URL:      *url,
			SkipSSL:  *skipSSL,
			Timeout:  *timeout,
		},
		StressArgs: args.StressArgs{
			Duration:       *duration,
			MaxConcurrency: *maxConcurrency,
			JSONConfigPath: *jsonConfigPath,
		},
		Protocol: *protocolMethod,
		Verbose:  *verbose,
	}, nil
}

// Execute is the entry point function after the args have been parsed
func Execute(args args.Args) error {
	fmt.Fprintf(os.Stdout, "dremio-stress %s %s-%s\n", odbcDisabled, Version, GitSha)
	engine, err := GetEngine(args)
	if err != nil {
		return err
	}
	return ExecuteWithEngine(args, engine, OsFileReader{})
}

// GetEngine is a function which returns a specific protocol engine
// based on the protocol specified in the passed configuration arguments.
func GetEngine(args args.Args) (protocol.Engine, error) {
	//loop through and match on available protocols
	for name, factoryFunction := range conf.GetProtocols() {
		if strings.ToLower(args.Protocol) == name {
			return factoryFunction(args.ProtocolArgs)
		}
	}
	// If the protocol is neither HTTP nor ODBC, return an error.
	return nil, fmt.Errorf("unknown protocol %v", args.Protocol)
}

// ExecuteWithEngine is the entry point function after the args have been parsed.
func ExecuteWithEngine(args args.Args, protocolEngine protocol.Engine, fileReader FileReader) (err error) {

	defer func() {
		if err := protocolEngine.Close(); err != nil {
			log.Printf("WARN: unable to close protocol engine '%v' due to %v", protocolEngine.Name(), err)
		}
	}()

	jsonText, err := fileReader.ReadFile(args.StressArgs.JSONConfigPath)
	if err != nil {
		return err
	}
	stressConf, err := conf.ParseStressJson(string(jsonText))
	if err != nil {
		return err
	}
	queryGen := gen.NewStressConfQueryGenerator(stressConf)
	return stress.Run(args.Verbose, protocolEngine, queryGen, args.StressArgs)
}

var examples = make(map[string]string)
var odbcDisabled = "odbc disabled"
var GitSha = "unknown"
var Version = "dev"

func init() {
	examples["Usage with http"] = "dremio-stress -user dremio -password dremio123 -url http://localhost:9047 -conf ./stress.json"
	examples["Usage with docker against a localhost dremio - (all dependencies bundled)"] = "docker run -it -v $(pwd):/mnt ghcr.io/rsvihladremio/dremio-stress-protocol odbc -user dremio -password dremio123 -url \"Driver={Arrow Flight SQL ODBC Driver};ConnectionType=Direct;AuthenticationType=Plain;Host=host.docker.internal;Port=32010;useEncryption=false\"  -conf /mnt/stress.json"
}
