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
	db             map[string]*sql.DB
	currentContext string
	lock           *sync.Mutex
}

func (o *ODBCProtocolEngine) Execute(query string, sqlContexts []string) error {
	sqlContext := strings.Join(sqlContexts, ".")
	slog.Warn("context", "context", sqlContext)
	o.lock.Lock()
	if len(sqlContext) > 0 { //&& o.currentContext != sqlContext {
		defer o.lock.Unlock()
		slog.Warn("changing context", "context", sqlContext)
		//o.currentContext = sqlContext
		rows, err := o.db[""].Query("USE " + sqlContext)
		if err != nil {
			return fmt.Errorf("failed executing USE context %v: %v", sqlContext, err)
		}
		rows.Close()
		rows, err = o.db[""].Query(query)
		if err != nil {
			return fmt.Errorf("failed executing query: %w", err)
		}
		rows.Close()
	} else {
		o.lock.Unlock()
		rows, err := o.db[""].Query(query)
		if err != nil {
			return fmt.Errorf("failed executing query: %w", err)
		}
		rows.Close()
	}
	return nil
}

// Close releases all resources related to the ODBC client connection func (o *ODBCProtocolEngine) Close() error {
func (o *ODBCProtocolEngine) Close() error {
	if o.db != nil {
		for _, v := range o.db {
			//TODO stop leaking
			v.Close()
		}
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
	connections := make(map[string]*sql.DB)
	/*for _, c := range contexts {
		db, err := sql.Open("odbc", dsn)
		if err != nil {
			return nil, fmt.Errorf("failed to open database %w", err)
		}
		if _, err := db.Exec("USE " + c); err != nil {
			return nil, err
		}
		connections[c] = db
	}
	*/
	db, err := sql.Open("odbc", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to open database %w", err)
	}
	connections[""] = db

	lock := &sync.Mutex{}
	return &ODBCProtocolEngine{
		db:   connections,
		lock: lock,
	}, nil
}
