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
