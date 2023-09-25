//go:build odbc

package protocol

import (
	"database/sql"
	"fmt"

	_ "github.com/alexbrainman/odbc"
	"github.com/rsvihladremio/dremio-stress/pkg/args"
)

// ODBCProtocolEngine uses ODBC calls using one of the two Dremio ODBC Drivers. The best supported is the Dremio Flight driver
type ODBCProtocolEngine struct {
	db *sql.DB
}

func (o *ODBCProtocolEngine) Execute(query string) error {
	_, err := o.db.Exec(query)
	if err != nil {
		return fmt.Errorf("failed executing query: %w", err)
	}
	return nil
}

// Close releases all resources related to the ODBC client connection
func (o *ODBCProtocolEngine) Close() error {
	return o.db.Close()
}

// Name of the protocol
func (o *ODBCProtocolEngine) Name() string {
	return "ODBC"
}

// NewODBCEngine creates a object capable of making calls using the Dremio ODBC API
func NewODBCEngine(a args.ProtocolArgs) (*ODBCProtocolEngine, error) {
	dsn := fmt.Sprintf(
		"%v;UID=%s;PWD=%s",
		a.URL,
		a.User,
		a.Password,
	)

	db, err := sql.Open("odbc", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to open database: %w", err)
	}

	return &ODBCProtocolEngine{
		db: db,
	}, nil
}
