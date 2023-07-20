package querygen

import (
	"fmt"
	"math/rand"
	"strings"

	"github.com/rsvihladremio/dremio-stress/pkg/conf"
)

type QueryGenerator interface {
	Query() ([]string, error)
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
	QueryList []string
}

func (s *StressConfQueryGenerator) Query() ([]string, error) {
	pick := rand.Intn(s.totalFreq)
	for _, q := range s.queries {
		if pick >= q.Range.Min && pick < q.Range.NextNumber {
			return q.QueryList, nil
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
		var sqls []string
		if q.QueryText == nil {
			if q.QueryGroup != nil {
				for _, group := range stressConf.QueryGroups {
					if group.Name == *q.QueryGroup {
						if len(group.Queries) == 0 {
							panic(fmt.Sprintf("invalid json: cannot have zero queries for query group %v", group.Name))
						}
						sqls = append(sqls, group.Queries...)
					}
				}
			} else {
				panic("neither \"queryGroup\" AND \"query\" are both empty, I cannot build a stress runtime from this json")
			}
		} else {
			sqls = append(sqls, *q.QueryText)
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
