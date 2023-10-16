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
	"strings"

	"github.com/rsvihladremio/dremio-stress/pkg/args"
	"github.com/rsvihladremio/dremio-stress/pkg/constants"
	"github.com/rsvihladremio/dremio-stress/pkg/protocol"
	"log/slog"
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
	SqlContext []string                 `json:"sqlContext,omitempty"`
	Frequency  int                      `json:"frequency"`
	Parameters map[string][]interface{} `json:"parameters"`
}

// StressJsonConf is the top level object that represents the stress.json object converted into a Go struct
type StressJsonConf struct {
	Queries     []QueryConf  `json:"queries"`
	QueryGroups []QueryGroup `json:"queryGroups,omitempty"`
}

func (s *StressJsonConf) GetContexts() []string {
	var contexts []string
	for _, q := range s.Queries {
		var builder strings.Builder
		for _, c := range q.SqlContext {
			tmp := c
			if strings.Contains(c, ".") {
				tmp = fmt.Sprintf("\"%v\"", c)
			}
			builder.WriteString(tmp)
		}
		contexts = append(contexts, builder.String())
	}
	return contexts
}

// ParseStressJson reads some jsonText and converts it into a StressJsonConf object, if
// the json is not valid it will return an error
func ParseStressJson(jsonText string) (StressJsonConf, error) {
	var stressConf StressJsonConf
	if err := json.Unmarshal([]byte(jsonText), &stressConf); err != nil {
		return StressJsonConf{}, fmt.Errorf("unable create configuration object: %v", err)
	}
	slog.Debug("configuration parsed", "source", jsonText, "result", stressConf)
	return stressConf, nil
}

var protocols = make(map[string]func(args.ProtocolArgs, []string) (protocol.Engine, error))

func init() {
	protocols[constants.HTTP] = func(args args.ProtocolArgs, contexts []string) (protocol.Engine, error) {
		// Try to create a new HTTP protocol engine.
		protocolEngine, err := protocol.NewHTTPEngine(args)
		if err != nil {
			// If there was an error creating the HTTP protocol engine, return an error.
			return nil, fmt.Errorf("unable to initialize HTTP protocol engine: %w", err)
		}
		// Return the created HTTP protocol engine.
		return protocolEngine, nil
	}
}

func GetProtocols() map[string]func(args.ProtocolArgs, []string) (protocol.Engine, error) {
	return protocols
}
