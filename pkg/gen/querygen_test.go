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

// package gen_test validates the gen package
package gen_test

import (
	"testing"

	"github.com/rsvihladremio/dremio-stress/pkg/conf"
	"github.com/rsvihladremio/dremio-stress/pkg/gen"
)

func TestQueryGenerationWithParams(t *testing.T) {
	originalQuery := "SELECT * FROM TEST WHERE my_date > ':my_date'"
	stressConf := conf.StressJsonConf{
		Queries: []conf.QueryConf{
			{
				QueryText: &originalQuery,
				Frequency: 1,
				Parameters: map[string][]interface{}{
					"my_date": {"2022-01-10"},
				},
			},
		},
	}
	generator := gen.NewStressConfQueryGenerator(stressConf)
	queries, err := generator.Queries()
	if err != nil {
		t.Fatalf("generator was unable to return queries, so stopping %v", err)
	}
	if len(queries) != 1 {
		t.Fatalf("expected 1 query but got %v", len(queries))
	}

	query := queries[0]
	expected := "SELECT * FROM TEST WHERE my_date > '2022-01-10'"
	if query != expected {
		t.Errorf("expected '%v' but got %v", expected, query)
	}
}
func TestQueryGenerationWithSeveralParams(t *testing.T) {
	originalQuery := "SELECT * FROM TEST WHERE my_date > ':my_date'"
	stressConf := conf.StressJsonConf{
		Queries: []conf.QueryConf{
			{
				QueryText: &originalQuery,
				Frequency: 1,
				Parameters: map[string][]interface{}{
					"my_date": {"2022-01-10", "2022-01-11", "2022-01-12"},
				},
			},
		},
	}
	generator := gen.NewStressConfQueryGenerator(stressConf)

	var allQueries []string
	for i := 0; i < 100; i++ {

		queries, err := generator.Queries()
		if err != nil {
			t.Fatalf("generator was unable to return queries, so stopping %v", err)
		}
		allQueries = append(allQueries, queries...)
	}
	var foundFirstParam bool
	var foundSecondParam bool
	var foundThirdParam bool
	for _, q := range allQueries {
		if q == "SELECT * FROM TEST WHERE my_date > '2022-01-10'" {
			foundFirstParam = true
		}
		if q == "SELECT * FROM TEST WHERE my_date > '2022-01-11'" {
			foundSecondParam = true
		}
		if q == "SELECT * FROM TEST WHERE my_date > '2022-01-12'" {
			foundThirdParam = true
		}
	}
	if !foundFirstParam {
		t.Error("first parameter not found in list of queries")
	}
	if !foundSecondParam {
		t.Error("second parameter not found in list of queries")
	}
	if !foundThirdParam {
		t.Error("third parameter not found in list of queries")
	}
}

func TestQueryGenerationWithNoParams(t *testing.T) {
	expectedQueryText := "SELECT * FROM TEST"
	stressConf := conf.StressJsonConf{
		Queries: []conf.QueryConf{
			{
				QueryText: &expectedQueryText,
				Frequency: 1,
			},
		},
	}
	generator := gen.NewStressConfQueryGenerator(stressConf)
	queries, err := generator.Queries()
	if err != nil {
		t.Fatalf("generator was unable to return queries, so stopping %v", err)
	}
	if len(queries) != 1 {
		t.Fatalf("expected 1 query but got %v", len(queries))
	}

	query := queries[0]
	if query != "SELECT * FROM TEST" {
		t.Errorf("expected 'SELECT * FROM TEST' but got %v", query)
	}
}

func TestQueryGenerationWithNoParamsAndUsingQueryGroup(t *testing.T) {
	expectedQueryGroupText := "queryGroup1"
	stressConf := conf.StressJsonConf{
		QueryGroups: []conf.QueryGroup{
			{
				Name: expectedQueryGroupText,
				Queries: []string{
					"SELECT * FROM TEST1",
					"SELECT * FROM TEST2",
					"SELECT * FROM TEST3",
				},
			},
		},
		Queries: []conf.QueryConf{
			{
				QueryGroup: &expectedQueryGroupText,
				Frequency:  1,
			},
		},
	}
	generator := gen.NewStressConfQueryGenerator(stressConf)
	queries, err := generator.Queries()
	if err != nil {
		t.Fatalf("generator was unable to return queries, so stopping %v", err)
	}

	if len(queries) != 3 {
		t.Fatalf("expected 3 queries but got %v", len(queries))
	}

	query := queries[0]
	if query != "SELECT * FROM TEST1" {
		t.Errorf("expected 'SELECT * FROM TEST1' but got %v", query)
	}

	query = queries[1]
	if query != "SELECT * FROM TEST2" {
		t.Errorf("expected 'SELECT * FROM TEST2' but got %v", query)
	}

	query = queries[2]
	if query != "SELECT * FROM TEST3" {
		t.Errorf("expected 'SELECT * FROM TEST3' but got %v", query)
	}
}

func TestQueryGenerationWithParamsAndUsingQueryGroup(t *testing.T) {
	expectedQueryGroupText := "queryGroup1"
	stressConf := conf.StressJsonConf{
		QueryGroups: []conf.QueryGroup{
			{
				Name: expectedQueryGroupText,
				Queries: []string{
					"SELECT * FROM TEST1 WHERE my_date > ':my_date'",
					"SELECT * FROM TEST2 WHERE my_date < ':my_date'",
					"SELECT * FROM TEST3 WHERE a > :my and my_date > ':my_date'",
				},
			},
		},
		Queries: []conf.QueryConf{
			{
				QueryGroup: &expectedQueryGroupText,
				Frequency:  1,
				Parameters: map[string][]interface{}{
					"my":      {1},
					"my_date": {"2014-01-10"},
				},
			},
		},
	}
	generator := gen.NewStressConfQueryGenerator(stressConf)
	queries, err := generator.Queries()
	if err != nil {
		t.Fatalf("generator was unable to return queries, so stopping %v", err)
	}

	if len(queries) != 3 {
		t.Fatalf("expected 3 queries but got %v", len(queries))
	}

	query := queries[0]
	if query != "SELECT * FROM TEST1 WHERE my_date > '2014-01-10'" {
		t.Errorf("expected 'SELECT * FROM TEST1 WHERE my_date > '2014-01-10'' but got %v", query)
	}

	query = queries[1]
	if query != "SELECT * FROM TEST2 WHERE my_date < '2014-01-10'" {
		t.Errorf("expected 'SELECT * FROM TEST2 WHERE my_date < '2014-01-10'' but got %v", query)
	}

	query = queries[2]
	if query != "SELECT * FROM TEST3 WHERE a > 1 and my_date > '2014-01-10'" {
		t.Errorf("expected 'SELECT * FROM TEST3 WHERE a > 1 and my_date > '2014-01-10'' but got %v", query)
	}
}
