package stress

import (
	"log"
	"sync"
	"time"

	"github.com/rsvihladremio/dremio-stress/pkg/conf"
	"github.com/rsvihladremio/dremio-stress/pkg/protocol"
	"github.com/rsvihladremio/dremio-stress/pkg/querygen"
)

func Run(verbose bool, protocolEngine protocol.Engine, queryGen querygen.QueryGenerator, args conf.StressArgs) error {

	// Create channel for jobs
	jobs := make(chan []string)

	// Number of workers (concurrency limit)
	maxConcurrency := args.MaxConcurrency

	// WaitGroup to wait for all workers to finish
	var wg sync.WaitGroup

	// Start the workers
	for i := 1; i <= maxConcurrency; i++ {
		wg.Add(1)
		go processor(i, verbose, protocolEngine, jobs, &wg)
	}

	// Set the maximum duration for generating queries (e.g., 5 seconds)
	maxDuration := args.Duration

	go func() {
		startTime := time.Now()

		// Generate and send strings to the jobs channel until the maximum duration is reached
		for {
			elapsed := time.Since(startTime)
			if elapsed >= maxDuration {
				break // Break the loop if the maximum duration is reached
			}

			job, err := queryGen.Query()
			if err != nil {
				log.Printf("ERROR: unable to get queries: %v", err)
			}
			if len(job) == 0 {
				break // Break the loop if there are no more strings to generate
			}
			jobs <- job
		}
		close(jobs) // Close the jobs channel when all strings are sent
	}()

	// Wait for all workers to finish processing
	wg.Wait()
	return nil
}

func processor(id int, verbose bool, protocolEngine protocol.Engine, jobs <-chan []string, wg *sync.WaitGroup) {
	defer wg.Done()
	for job := range jobs {
		if verbose {
			log.Printf("DEBUG: worker %v firing query `%v`", id, job)
		}
		total := len(job)
		for i, q := range job {
			if err := protocolEngine.Execute(q); err != nil {
				log.Printf("ERROR: query '%v' failed: '%v'", job, err)
				//stop bothering trying to execute the rest..if a query group fails we should just stop
				remaining := total - (i + 1)
				if remaining > 0 {
					log.Printf("WARN: skipping remaining %v queries", remaining)
				}
				break
			}
		}
	}
}
