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

// Package stress is the runtime that coordinates threads to cap load to the max-concurrency
// it also has to orchestrate passing queries from the query generator to the protocol engine
package stress

import (
	"fmt"
	"log/slog"
	"sync"
	"sync/atomic"
	"time"

	"github.com/rsvihladremio/dremio-stress/pkg/args"
	"github.com/rsvihladremio/dremio-stress/pkg/gen"
	"github.com/rsvihladremio/dremio-stress/pkg/protocol"
)

func Run(protocolEngine protocol.Engine, queryGen gen.QueryGenerator, args args.StressArgs) error {

	// Create channel for queriesChan
	queriesChan := make(chan []string)

	// Number of workers (concurrency limit)
	maxConcurrency := args.MaxConcurrency

	// WaitGroup to wait for all workers to finish
	var wg sync.WaitGroup

	// Start the workers
	for i := 1; i <= maxConcurrency; i++ {
		wg.Add(1)
		go processor(i, protocolEngine, queriesChan, &wg)
	}

	// Set the maximum duration for generating queries (e.g., 5 seconds)
	maxDuration := args.Duration

	var ops uint64
	go func() {
		startTime := time.Now()
		go func() {
			pauseTime := 10 * time.Second
			timer := time.NewTicker(pauseTime)
			for {
				timer.Reset(pauseTime)
				<-timer.C //block for 10 seconds
				elapsed := time.Since(startTime)
				if elapsed >= maxDuration {
					timer.Stop()
					break // Break the loop if the maximum duration is reached
				}
				fmt.Printf("%v queries submitted and %v seconds remaining\n", atomic.LoadUint64(&ops), int64((maxDuration - elapsed).Seconds()))
			}
		}()

		// Generate and send strings to the queries channel until the maximum duration is reached
		for {
			elapsed := time.Since(startTime)
			if elapsed >= maxDuration {
				break // Break the loop if the maximum duration is reached
			}

			queries, err := queryGen.Queries()
			if err != nil {
				slog.Error(" unable to get queries", "error_msg", err)
			}
			if len(queries) == 0 {
				break // Break the loop if there are no more strings to generate
			}
			queriesChan <- queries
			atomic.AddUint64(&ops, 1)
		}
		close(queriesChan) // Close the queries channel when all strings are sent
	}()

	// Wait for all workers to finish processing
	wg.Wait()
	return nil
}

func processor(id int, protocolEngine protocol.Engine, jobs <-chan []string, wg *sync.WaitGroup) {
	defer wg.Done()
	for job := range jobs {
		slog.Debug("worker firing query", "worker_id", id, "query", job)
		total := len(job)
		for i, q := range job {
			if err := protocolEngine.Execute(q); err != nil {
				slog.Error("query failed", "query", job, "error_msg", err)
				//stop bothering trying to execute the rest..if a query group fails we should just stop
				remaining := total - (i + 1)
				if remaining > 0 {
					slog.Warn("skipping remaining queries", "queries_remaining", remaining)
				}
				break
			}
		}
	}
}
