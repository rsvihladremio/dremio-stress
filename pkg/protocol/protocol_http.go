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

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"time"

	"github.com/rsvihladremio/dremio-stress/pkg/args"
)

// Engine provides the interface for making remote calls to dremio via a given protocol
type Engine interface {
	Execute(string) error
	Name() string
	Close() error
}

// HTTPProtocolEngine uses HTTP calls against the Dremio REST API
type HTTPProtocolEngine struct {
	token               string
	client              http.Client
	queryTimeoutMinutes int
	queryURL            string
	queryStatusURL      string
}

// Close is no-op for the HTTPProtocolEngine and will always succeed
func (h *HTTPProtocolEngine) Close() error {
	return nil
}

// Name of the protocol
func (h *HTTPProtocolEngine) Name() string {
	return "HTTP"
}

func (h *HTTPProtocolEngine) Execute(query string) error {
	data := map[string]string{
		"sql": query,
	}
	jsonBody, err := json.Marshal(data)
	if err != nil {
		return fmt.Errorf("unable to create sql json: %w", err)
	}
	req, err := http.NewRequest(http.MethodPost, h.queryURL, bytes.NewBuffer(jsonBody))
	if err != nil {
		return fmt.Errorf("unable to create request %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", h.token)

	res, err := h.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed sending login request: %w", err)
	}
	resBody, err := io.ReadAll(res.Body)
	if err != nil {
		return fmt.Errorf("could not read response body: %w", err)
	}
	var resultMap map[string]interface{}
	err = json.Unmarshal(resBody, &resultMap)
	if err != nil {
		return fmt.Errorf("could not read %v json %w", string(resBody), err)
	}

	v, ok := resultMap["id"]
	if ok {
		token := fmt.Sprintf("%v", v)
		if token == "" {
			return errors.New("blank id cannot proceed")
		}
		// TODO: add stats on job status at some point
		_, err = h.checkQueryStatus(fmt.Sprintf("%v", v))
		if err != nil {
			return err
		}
		return nil
	}
	return fmt.Errorf("no job id in response %#v so failing the query", resultMap)
}

func (h *HTTPProtocolEngine) checkQueryStatus(id string) (status string, err error) {
	url := fmt.Sprintf("%v/%v", h.queryStatusURL, id)
	intervalsPerMinutes := 120
	sleepTimeSeconds := 60.0 / intervalsPerMinutes
	totalIterations := h.queryTimeoutMinutes * intervalsPerMinutes
	var lastState string
	for i := 0; i < totalIterations; i++ {
		time.Sleep(time.Duration(sleepTimeSeconds) * time.Second)
		req, err := http.NewRequest(http.MethodGet, url, nil)
		if err != nil {
			return "", fmt.Errorf("unable to create request %w", err)
		}
		req.Header.Set("Content-Type", "application/json")
		req.Header.Set("Authorization", h.token)

		res, err := h.client.Do(req)
		if err != nil {
			return "", fmt.Errorf("failed sending login request: %w", err)
		}
		resBody, err := io.ReadAll(res.Body)
		if err != nil {
			return "", fmt.Errorf("client: could not read response body: %s", err)
		}
		var resultMap map[string]interface{}
		err = json.Unmarshal(resBody, &resultMap)
		if err != nil {
			return "", fmt.Errorf("client: could not read json: %s", err)
		}

		if jobState, ok := resultMap["jobState"]; ok {
			v := fmt.Sprintf("%v", jobState)
			// possible results
			//"NOT_SUBMITTED, STARTING, RUNNING, COMPLETED, CANCELED, FAILED, CANCELLATION_REQUESTED, PLANNING, PENDING, METADATA_RETRIEVAL, QUEUED, ENGINE_START, EXECUTION_PLANNING, INVALID_STATE
			if v == "COMPLETED" || v == "CANCELLED" || v == "FAILED" || v == "INVALID_STATE" || v == "CANCELLATION_REQUESTED" || v == "" {
				slog.Debug("query done", "query_time_seconds", (i+1)*sleepTimeSeconds)
				return v, nil
			}
			token := fmt.Sprintf("%v", v)
			if token == "" {
				return "", errors.New("blank id cannot proceed")
			}
			lastState = v
		} else {
			return "", fmt.Errorf("invalid result body for id %v: %#v", id, resultMap)
		}
	}
	return lastState, fmt.Errorf("query timed out after %v minutes. state was %v", h.queryTimeoutMinutes, lastState)
}

// NewHTTPEngine creates the object capable of making calls against the Dremio REST API
func NewHTTPEngine(a args.ProtocolArgs) (*HTTPProtocolEngine, error) {
	client, token, err := authenticateHTTP(a)
	if err != nil {
		return &HTTPProtocolEngine{}, err
	}
	return &HTTPProtocolEngine{
		token:               fmt.Sprintf("_dremio%v", token),
		queryURL:            fmt.Sprintf("%v/api/v3/sql", a.URL),
		queryStatusURL:      fmt.Sprintf("%v/api/v3/job", a.URL),
		client:              client,
		queryTimeoutMinutes: 60,
	}, nil
}

func authenticateHTTP(a args.ProtocolArgs) (http.Client, string, error) {
	var err error
	client := http.Client{
		Timeout: 30 * time.Second,
	}
	baseURL := a.URL
	jsonBody := []byte(fmt.Sprintf(`{"userName": "%v", "password":"%v"}`, a.User, a.Password))
	bodyReader := bytes.NewReader(jsonBody)
	req, err := http.NewRequest(http.MethodPost, fmt.Sprintf("%v/apiv2/login", baseURL), bodyReader)
	if err != nil {
		return client, "", fmt.Errorf("unable to create request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")

	res, err := client.Do(req)
	if err != nil {
		return client, "", fmt.Errorf("failed sending login request: %w", err)
	}
	resBody, err := io.ReadAll(res.Body)
	if err != nil {
		return client, "", fmt.Errorf("client: could not read response body: %w", err)
	}
	var resultMap map[string]interface{}
	err = json.Unmarshal(resBody, &resultMap)
	if err != nil {
		return client, "", fmt.Errorf("could not read json '%v' when logging in due to error '%w'", string(resBody), err)
	}

	if v, ok := resultMap["token"]; ok {
		token := fmt.Sprintf("%v", v)
		if token == "" {
			return client, "", errors.New("blank token cannot proceed")
		}
		return client, token, nil
	}
	return client, "", fmt.Errorf("unable to read token from %#v", resultMap)
}
