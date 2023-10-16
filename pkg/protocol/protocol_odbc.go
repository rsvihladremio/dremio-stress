//go:build odbc

package protocol

import (
	"database/sql"
	"fmt"
	"log/slog"
	"strings"
	"sync"

	_ "github.com/alexbrainman/odbc"
	"github.com/rsvihladremio/dremio-stress/pkg/args"
)

// ODBCProtocolEngine uses ODBC calls using one of the two Dremio ODBC Drivers. The best supported is the Dremio Flight driver
type ODBCProtocolEngine struct {
	db             *sql.DB
	currentContext string
	lock           *sync.Mutex
}

func (o *ODBCProtocolEngine) Execute(query string, sqlContext []string) error {
	slog.Warn("context", "context", sqlContext)
	o.lock.Lock()
	//if sqlContext != "" && o.currentContext != sqlContext {
	//		defer o.lock.Unlock()
	//		o.currentContext = sqlContext
	//_, err := o.db.Exec(fmt.Sprintf("USE %v", sqlContext))
	//if err != nil {
	//return fmt.Errorf("failed executing query: %w", err)
	//}
	if len(sqlContext) > 0 {
		defer o.lock.Unlock()
		useSt := fmt.Sprintf("USE %v", strings.Join(sqlContext, "."))
		//slog.Warn("changing context", "context", sqlContext)
		_, err := o.db.Exec(useSt + ";" + query)
		if err != nil {
			return fmt.Errorf("failed executing query: %w", err)
		}
	} else {
		o.lock.Unlock()
		_, err := o.db.Exec(query)
		if err != nil {
			return fmt.Errorf("failed executing query: %w", err)
		}
	}
	return nil
}

// Close releases all resources related to the ODBC client connection func (o *ODBCProtocolEngine) Close() error {
func (o *ODBCProtocolEngine) Close() error {
	if o.db != nil {
		return o.db.Close()
	}
	return nil
}

// Name of the protocol
func (o *ODBCProtocolEngine) Name() string {
	return "ODBC"
}

// NewODBCEngine creates a object capable of making calls using the Dremio ODBC API
func NewODBCEngine(a args.ProtocolArgs, contexts []string) (*ODBCProtocolEngine, error) {
	dsn := fmt.Sprintf(
		"%v;UID=%s;PWD=%s",
		a.URL,
		a.User,
		a.Password,
	)
	db, err := sql.Open("odbc", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to open database %w", err)
	}
	lock := &sync.Mutex{}
	return &ODBCProtocolEngine{
		db:   db,
		lock: lock,
	}, nil
}
