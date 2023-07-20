package conf

import (
	"encoding/json"
	"time"
)

type QueryGroup struct {
	Name    string   `json:"name"`
	Queries []string `json:"queries"`
}

type QueryConf struct {
	QueryText  *string                  `json:"query,omitempty"`
	QueryGroup *string                  `json:"queryGroup,omitempty"`
	Frequency  int                      `json:"frequency"`
	Parameters map[string][]interface{} `json:"-"`
}

type StressJsonConf struct {
	Queries     []QueryConf  `json:"queries"`
	QueryGroups []QueryGroup `json:"queryGroups,omitempty"`
}

func ParseStressJson(jsonText string) (StressJsonConf, error) {
	var stressConf StressJsonConf
	if err := json.Unmarshal([]byte(jsonText), &stressConf); err != nil {
		return StressJsonConf{}, err
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
	User     string
	Password string
	URL      string
	SkipSSL  bool
	Timeout  time.Duration
}

// StressArgs provites a way to configure the stress runtime
type StressArgs struct {
	Duration       time.Duration
	MaxConcurrency int
	YAMLConfigPath string
}

// Protocol provides the communication protocol used
type Protocol int

const (
	HTTP Protocol = iota
	ODBC
)
