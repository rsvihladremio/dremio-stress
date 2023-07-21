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

// Package protocol provides the implementation for sending queries to Dremio
package protocol

import "github.com/rsvihladremio/dremio-stress/pkg/conf"

// Engine provides the interface for making remote calls to dremio via a given protocol
type Engine interface {
	Execute(string) error
}

// HTTPProtocolEngine uses HTTP calls against the Dremio REST API
type HTTPProtocolEngine struct {
}

func (h *HTTPProtocolEngine) Execute(query string) error {
	return nil
}

// ODBCProtocolEngine uses ODBC calls using one of the two Dremio ODBC Drivers. The best supported is the Dremio Flight driver
type ODBCProtocolEngine struct {
}

func (o *ODBCProtocolEngine) Execute(query string) error {
	return nil
}

// NewHTTPEngine creates the object capable of making calls against the Dremio REST API
func NewHTTPEngine(a conf.ProtocolArgs) *HTTPProtocolEngine {
	return &HTTPProtocolEngine{}
}

// NewODBCEngine creates a object capable of making calls using the Dremio ODBC API
func NewODBCEngine(a conf.ProtocolArgs) *ODBCProtocolEngine {
	return &ODBCProtocolEngine{}
}
