//go:build odbc

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

package conf

import (
	"fmt"

	"github.com/rsvihladremio/dremio-stress/pkg/args"
	"github.com/rsvihladremio/dremio-stress/pkg/constants"
	"github.com/rsvihladremio/dremio-stress/pkg/protocol"
)

func init() {
	protocols[constants.ODBC] = func(args args.ProtocolArgs, contexts []string) (protocol.Engine, error) {
		// initialize the ODBC protocol engine.
		protocolEngine, err := protocol.NewODBCEngine(args, contexts)
		if err != nil {
			// If there was an error creating the ODBC protocol engine, return an error.
			return nil, fmt.Errorf("unable to initialize ODBC protocol engine: %w", err)
		}
		// Return the created ODBC protocol engine.
		return protocolEngine, nil
	}
}
