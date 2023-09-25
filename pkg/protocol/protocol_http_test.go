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

// Package protocol_test validates the protocol package
package protocol_test

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/ory/dockertest/v3"
	"github.com/ory/dockertest/v3/docker"
	"github.com/rsvihladremio/dremio-stress/pkg/args"
	"github.com/rsvihladremio/dremio-stress/pkg/protocol"
)

var baseURL string

func TestMain(m *testing.M) {
	// uses a sensible default on windows (tcp/http) and linux/osx (socket)
	pool, err := dockertest.NewPool("")
	if err != nil {
		log.Fatalf("Could not construct pool: %s", err)
	}

	// uses pool to try to connect to Docker
	err = pool.Client.Ping()
	if err != nil {
		log.Fatalf("Could not connect to Docker: %s", err)
	}
	// pulls an image, creates a container based on it and runs it
	resource, err := pool.RunWithOptions(
		&dockertest.RunOptions{
			Repository:   "ghcr.io/rsvihladremio/dremio-oss-test-image",
			Tag:          "latest",
			ExposedPorts: []string{"9047"},
		}, func(config *docker.HostConfig) {
			config.AutoRemove = true
		})
	if err != nil {
		log.Fatalf("Could not start resource: %s", err)
	}

	port, err := strconv.Atoi(resource.GetPort("9047/tcp"))
	if err != nil {
		log.Fatalf("Could not get host port: %s", err)
	}
	// exponential backoff-retry, because the application in the container might not be ready to accept connections yet
	if err := pool.Retry(func() error {
		var err error
		host := "localhost"
		baseURL = fmt.Sprintf("http://%v:%d", host, port)
		jsonBody := []byte(`{"userName": "dremio", "password":"dremio123"}`)
		bodyReader := bytes.NewReader(jsonBody)
		req, err := http.NewRequest(http.MethodPost, fmt.Sprintf("%v/apiv2/login", baseURL), bodyReader)
		if err != nil {
			return fmt.Errorf("unable to create request %w", err)
		}
		req.Header.Set("Content-Type", "application/json")

		client := http.Client{
			Timeout: 30 * time.Second,
		}

		res, err := client.Do(req)
		if err != nil {
			return fmt.Errorf("failed sending login request: %w", err)
		}
		resBody, err := io.ReadAll(res.Body)
		if err != nil {
			return fmt.Errorf("client: could not read response body: %w", err)
		}
		var resultMap map[string]interface{}
		err = json.Unmarshal(resBody, &resultMap)
		if err != nil {
			return fmt.Errorf("client: could not read json: %w", err)
		}

		if v, ok := resultMap["token"]; ok {
		} else {
			token := fmt.Sprintf("%v", v)
			if token == "" {
				return errors.New("blank token cannot proceed")
			}
		}
		return nil
	}); err != nil {
		log.Fatalf("Could not connect to dremio: %s", err)
	}

	code := m.Run()

	// You can't defer this because os.Exit doesn't care for defer
	if err := pool.Purge(resource); err != nil {
		log.Fatalf("Could not purge resource: %s", err)
	}

	os.Exit(code)
}

func TestHttpQuerySuccessOverHTTP(t *testing.T) {
	c := args.ProtocolArgs{
		User:     "dremio",
		Password: "dremio123",
		URL:      baseURL,
	}

	p, err := protocol.NewHTTPEngine(c)
	if err != nil {
		t.Fatal(logUnwrappedErrors(err))
	}
	if err = p.Execute("SELECT * FROM sys.project.jobs"); err != nil {
		t.Errorf("failed executing the query: %v", logUnwrappedErrors(err))
	}
}

func TestSqlQueryFailsOverHTTPWithWrongUserNameAndPass(t *testing.T) {
	c := args.ProtocolArgs{
		User:     "not right",
		Password: "this either",
		URL:      baseURL,
		Timeout:  10 * time.Second,
	}

	_, err := protocol.NewHTTPEngine(c)
	if err == nil {
		t.Fatal("expect an error on login since this is not a valid password")
	}
}

func logUnwrappedErrors(err error) string {
	var str strings.Builder
	for err != nil {
		if _, err := str.WriteString(fmt.Sprintf("Error:%+v\n", err)); err != nil {
			log.Fatalf("supposedly this doesn't happen ever when writing to a string builder %v", err)
		}
		err = errors.Unwrap(err)
	}
	return str.String()
}
