//	Copyright 2023 Dremio Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


// Package conf provides logic for parsing the stress.json
package conf

import (
	"encoding/json"
	"fmt"
	"time"
)

// QueryGroup is a sequence of queries run in order together
type QueryGroup struct {
	Name    string   `json:"name"`
	Queries []string `json:"queries"`
}

// QueryConf has the data necessary to provide query generators with enough data to create new queries
type QueryConf struct {
	QueryText  *string                  `json:"query,omitempty"`
	QueryGroup *string                  `json:"queryGroup,omitempty"`
	Frequency  int                      `json:"frequency"`
	Parameters map[string][]interface{} `json:"parameters"`
}

// StressJsonConf is the top level object that represents the stress.json object converted into a Go struct
type StressJsonConf struct {
	Queries     []QueryConf  `json:"queries"`
	QueryGroups []QueryGroup `json:"queryGroups,omitempty"`
}

// ParseStressJson reads some jsonText and converts it into a StressJsonConf object, if
// the json is not valid it will return an error
func ParseStressJson(jsonText string) (StressJsonConf, error) {
	var stressConf StressJsonConf
	if err := json.Unmarshal([]byte(jsonText), &stressConf); err != nil {
		return StressJsonConf{}, fmt.Errorf("unable create configuration object: %v", err)
	}
	return stressConf, nil
}

// Args provides the parseable arguments in dremio-stress
type Args struct {
	ProtocolArgs ProtocolArgs
	StressArgs   StressArgs
	Protocol     Protocol
	Verbose      bool
}

// ProtocolArgs provides a way to configure the communication protocol
type ProtocolArgs struct {
	User     string        // User for Dremio to ues to execute the queries in stress.json
	Password string        // Password for Dremio to use to execute the queries in stress.json
	URL      string        // URL either HTTP URL or ODBC connection string
	SkipSSL  bool          // SkipSSL avoids validating certificates and hostname for HTTPS or ODBC connections
	Timeout  time.Duration // Timeout duration for requests (in the case of HTTP will be each request, included checks for query status, ODBC is per query
}

// StressArgs provites a way to configure the stress runtime
type StressArgs struct {
	Duration       time.Duration // Duration of the entire stress run
	MaxConcurrency int           // MaxConcurrency is the maximum number of queries that can be hitting the cluster from the stress client
	JSONConfigPath string        // JSONConfigPath is where to find the stress.json
}

// Protocol provides the communication protocol used
type Protocol int

const (
	// HTTP Protocol means communication with Dremio will be over HTTP
	HTTP Protocol = iota
	// ODBC Protocol means communication with Dremio will be over ODBC
	ODBC
)
