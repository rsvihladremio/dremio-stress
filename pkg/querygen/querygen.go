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

// package querygen takes the stress.json conf and uses those rules to generate queries to send to Dremio
package querygen

import (
	"fmt"
	"math/rand"
	"regexp"
	"strings"

	"github.com/rsvihladremio/dremio-stress/pkg/conf"
)

type QueryWithParams struct {
	QueryText  string
	Parameters map[string][]interface{}
}

type QueryGenerator interface {
	Queries() ([]string, error)
}

type StressConfQueryGenerator struct {
	queries   []QueryMatcher
	totalFreq int
}

type ValidRange struct {
	Min        int
	NextNumber int
}

type QueryMatcher struct {
	Range     ValidRange
	QueryList []QueryWithParams
}

func pickParam(options []interface{}) interface{} {
	pick := rand.Intn(len(options))
	return options[pick]
}

func TokenMap(query string, replacements map[string][]interface{}) string {
	// Create a regular expression pattern to match tokens (e.g., :my and :my_date)
	pattern := `:(\w+)`

	// Define a function to replace tokens with values
	replaceFunc := func(match string) string {
		// Extract the token (e.g., :my) from the match
		token := strings.TrimPrefix(match, ":")

		// Lookup the replacement value in the map
		replacement, exists := replacements[token]

		// If a replacement exists, return it; otherwise, return the original match
		if exists {
			pick := pickParam(replacement)
			return fmt.Sprintf("%v", pick)
		}
		return match
	}

	// Use the regular expression to replace tokens with values
	return regexp.MustCompile(pattern).ReplaceAllStringFunc(query, replaceFunc)

}

func (s *StressConfQueryGenerator) Queries() ([]string, error) {
	pick := rand.Intn(s.totalFreq)
	for _, q := range s.queries {
		if pick >= q.Range.Min && pick < q.Range.NextNumber {
			var mappedSql []string
			for _, query := range q.QueryList {
				rawSql := query.QueryText
				if len(query.Parameters) > 0 {
					rawSql = TokenMap(rawSql, query.Parameters)
				}
				mappedSql = append(mappedSql, rawSql)
			}
			return mappedSql, nil
		}
	}

	var ranges []string
	for _, q := range s.queries {
		ranges = append(ranges, fmt.Sprintf("{start: %v, end: %v}", q.Range.Min, q.Range.NextNumber))
	}
	return []string{}, fmt.Errorf("the number %v did not find a list of ranges that matched out of: %v", pick, strings.Join(ranges, ", "))
}

func NewStressConfQueryGenerator(stressConf conf.StressJsonConf) *StressConfQueryGenerator {
	totalFreq := 0
	var queries []QueryMatcher
	for _, q := range stressConf.Queries {
		min := totalFreq
		totalFreq += q.Frequency
		nextNumber := totalFreq
		r := ValidRange{
			Min:        min,
			NextNumber: nextNumber,
		}
		var sqls []QueryWithParams
		if q.QueryText == nil {
			if q.QueryGroup != nil {
				for _, group := range stressConf.QueryGroups {
					if group.Name == *q.QueryGroup {
						if len(group.Queries) == 0 {
							panic(fmt.Sprintf("invalid json: cannot have zero queries for query group %v", group.Name))
						}
						for _, groupQueryText := range group.Queries {
							sqls = append(sqls, QueryWithParams{
								QueryText:  groupQueryText,
								Parameters: q.Parameters,
							})
						}
					}
				}
			} else {
				panic("neither \"queryGroup\" AND \"query\" are both empty, I cannot build a stress runtime from this json")
			}
		} else {
			text := *q.QueryText
			params := q.Parameters
			sqls = append(sqls, QueryWithParams{
				QueryText:  text,
				Parameters: params,
			})
		}
		queries = append(queries, QueryMatcher{
			Range:     r,
			QueryList: sqls,
		})
	}
	return &StressConfQueryGenerator{
		totalFreq: totalFreq,
		queries:   queries,
	}
}
