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

// conf_test tests the conf package
package conf_test

import (
	"fmt"
	"testing"

	"github.com/rsvihladremio/dremio-stress/pkg/conf"
)

// TestParseJsonStress validates all the fields are correctly mapped from a json file to the StressJsonConf
func TestParseJsonStress(t *testing.T) {

	jsonText := `{
  "queries": [
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv LIMIT 50\"",
      "frequency": 5
    },
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv\" where \"DATE\" between ':start' and ':end'",
      "frequency": 5,
      "parameters": {
        "start": ["2018-02-04","2018-02-05"],
        "end": ["2018-02-14","2018-02-15"]
      }
    }
  ]
}
`
	//parse the raw string
	stressConf, err := conf.ParseStressJson(jsonText)
	if err != nil {
		t.Fatalf("unable to parse stress.json example text with query group: %v", err)
	}

	// test if all the fields were mapped starting with the counts at the top

	queries := stressConf.Queries

	expected := 2
	actual := len(queries)
	if expected != actual {
		// subsequent assertions may fail if the count is not accurate. So stop here.
		t.Fatalf("expected %v but was %v", expected, actual)
	}
	queryGroups := stressConf.QueryGroups
	expected = 0
	actual = len(queryGroups)
	if expected != actual {
		// subsequent assertions may fail if the count is not accurate. So stop here.
		t.Fatalf("expected %v but was %v", expected, actual)
	}

	// now assert on the content of the first query, we're assuming they're parsed in order as they appeared in the json file
	query := queries[0]
	if query.Frequency != 5 {
		t.Errorf("expected a frequency of 1 but was %v", query.Frequency)
	}
	if query.QueryGroup != nil {
		t.Errorf("expected an empty query group but was %v", query.QueryGroup)
	}

	expectedQueryText := "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv LIMIT 50\""
	if *query.QueryText != expectedQueryText {
		t.Errorf("expected query text of %v but was %v", expectedQueryText, *query.QueryText)
	}

	if len(query.Parameters) != 0 {
		t.Errorf("we expected no query parameters but there were %v", len(query.Parameters))
	}

	// now assert on the content of the second query
	query = queries[1]
	if query.Frequency != 5 {
		t.Errorf("expected a frequency of 9 but was %v", query.Frequency)
	}
	if query.QueryGroup != nil {
		t.Errorf("expected an empty query group but was %v", query.QueryGroup)
	}

	expectedQueryText = "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv\" where \"DATE\" between ':start' and ':end'"
	if *query.QueryText != expectedQueryText {
		t.Errorf("expected a query text of %v but was %v", expectedQueryText, query.QueryText)
	}

	if len(query.Parameters) != 2 {
		//cannot do the next assertions if this fails so stopping here
		t.Fatalf("we expected 2 query parameters but there were %v", len(query.Parameters))
	}

	if parameter, ok := query.Parameters["start"]; !ok {
		t.Error("missing query parameter start")
	} else {
		if len(parameter) != 2 {
			t.Errorf("expected 2 parameter values but we had %v", len(parameter))
		}
		parameterValue := parameter[0]
		expectedDate := "2018-02-04"
		actualDate := fmt.Sprintf("%v", parameterValue)
		if actualDate != expectedDate {
			t.Errorf("expected %v but was %v", expectedDate, actualDate)
		}
		parameterValue = parameter[1]
		expectedDate = "2018-02-05"
		actualDate = fmt.Sprintf("%v", parameterValue)
		if actualDate != expectedDate {
			t.Errorf("expected %v but was %v", expectedDate, actualDate)
		}
	}
	if parameter, ok := query.Parameters["end"]; !ok {
		t.Error("missing query parameter end")
	} else {
		if len(parameter) != 2 {
			t.Errorf("expected 2 parameter values but we had %v", len(parameter))
		}
		parameterValue := parameter[0]
		expectedDate := "2018-02-14"
		actualDate := fmt.Sprintf("%v", parameterValue)
		if actualDate != expectedDate {
			t.Errorf("expected %v but was %v", expectedDate, actualDate)
		}
		parameterValue = parameter[1]
		expectedDate = "2018-02-15"
		actualDate = fmt.Sprintf("%v", parameterValue)
		if actualDate != expectedDate {
			t.Errorf("expected %v but was %v", expectedDate, actualDate)
		}
	}

}

// TestParseJsonStressWithQueryGroup validates all the fields are correctly mapped from a json file to the StressJsonConf
func TestParseJsonStressWithQueryGroup(t *testing.T) {

	jsonText := `{
  "queryGroups": [
    {
      "name": "schemaops",
      "queries": [
        "drop table if exits samples.\"samples.dremio.com\".\"nyc-taxi-trips\"",
        "create table samples.\"samples.dremio.com\".\"nyc-taxi-trips\" STORE AS (type => 'iceberg') AS SELECT (\"a\",\"b\" FROM (values('a', 'b')) as t(\"a\",\"b\"))",
        "select * from  samples.\"samples.dremio.com\".\"nyc-taxi-trips\""
      ]
    }
  ],
  "queries": [
    {
      "queryGroup": "schemaops",
      "frequency": 1
    },
    {
      "query": "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv\" where \"DATE\" between ':start' and ':end'",
      "frequency": 9,
      "parameters":{
        "start": [ "2018-02-04", "2018-02-05"],
        "end": ["2018-02-14","2018-02-15"]
      }
    }
  ]
}
`
	//parse the raw string
	stressConf, err := conf.ParseStressJson(jsonText)
	if err != nil {
		t.Fatalf("unable to parse stress.json example text with query group: %v", err)
	}

	// test if all the fields were mapped starting with the counts at the top

	queries := stressConf.Queries

	expected := 2
	actual := len(queries)
	if expected != actual {
		// subsequent assertions may fail if the count is not accurate. So stop here.
		t.Fatalf("expected %v but was %v", expected, actual)
	}
	queryGroups := stressConf.QueryGroups
	expected = 1
	actual = len(queryGroups)
	if expected != actual {
		// subsequent assertions may fail if the count is not accurate. So stop here.
		t.Fatalf("expected %v but was %v", expected, actual)
	}

	// now assert on the content of the first query, we're assuming they're parsed in order as they appeared in the json file
	query := queries[0]
	if query.Frequency != 1 {
		t.Errorf("expected a frequency of 1 but was %v", query.Frequency)
	}
	if *query.QueryGroup != "schemaops" {
		t.Errorf("expected a query group of 'schemaops' but was %v", query.QueryGroup)
	}

	if query.QueryText != nil {
		t.Errorf("expected an empty query text but was %v", query.QueryText)
	}

	if len(query.Parameters) != 0 {
		t.Errorf("we expected no query parameters but there were %v", len(query.Parameters))
	}

	// now assert on the content of the second query
	query = queries[1]
	if query.Frequency != 9 {
		t.Errorf("expected a frequency of 9 but was %v", query.Frequency)
	}
	if query.QueryGroup != nil {
		t.Errorf("expected an empty query group but was %v", query.QueryGroup)
	}

	expectedQueryText := "select * FROM Samples.\"samples.dremio.com\".\"SF weather 2018-2019.csv\" where \"DATE\" between ':start' and ':end'"
	if *query.QueryText != expectedQueryText {
		t.Errorf("expected a query text of %v but was %v", expectedQueryText, query.QueryText)
	}

	if len(query.Parameters) != 2 {
		//cannot do the next assertions if this fails so stopping here
		t.Fatalf("we expected 2 query parameters but there were %v", len(query.Parameters))
	}

	if parameter, ok := query.Parameters["start"]; !ok {
		t.Error("missing query parameter start")
	} else {
		if len(parameter) != 2 {
			t.Errorf("expected 2 parameter values but we had %v", len(parameter))
		}
		parameterValue := parameter[0]
		expectedDate := "2018-02-04"
		actualDate := fmt.Sprintf("%v", parameterValue)
		if actualDate != expectedDate {
			t.Errorf("expected %v but was %v", expectedDate, actualDate)
		}
		parameterValue = parameter[1]
		expectedDate = "2018-02-05"
		actualDate = fmt.Sprintf("%v", parameterValue)
		if actualDate != expectedDate {
			t.Errorf("expected %v but was %v", expectedDate, actualDate)
		}
	}
	if parameter, ok := query.Parameters["end"]; !ok {
		t.Error("missing query parameter end")
	} else {
		if len(parameter) != 2 {
			t.Errorf("expected 2 parameter values but we had %v", len(parameter))
		}
		parameterValue := parameter[0]
		expectedDate := "2018-02-14"
		actualDate := fmt.Sprintf("%v", parameterValue)
		if actualDate != expectedDate {
			t.Errorf("expected %v but was %v", expectedDate, actualDate)
		}
		parameterValue = parameter[1]
		expectedDate = "2018-02-15"
		actualDate = fmt.Sprintf("%v", parameterValue)
		if actualDate != expectedDate {
			t.Errorf("expected %v but was %v", expectedDate, actualDate)
		}
	}

}
