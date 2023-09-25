package args

import "time"

// Args provides the parsable arguments in dremio-stress
type Args struct {
	ProtocolArgs ProtocolArgs
	StressArgs   StressArgs
	Protocol     string
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

// StressArgs provides a way to configure the stress runtime
type StressArgs struct {
	Duration       time.Duration // Duration of the entire stress run
	MaxConcurrency int           // MaxConcurrency is the maximum number of queries that can be hitting the cluster from the stress client
	JSONConfigPath string        // JSONConfigPath is where to find the stress.json
}
