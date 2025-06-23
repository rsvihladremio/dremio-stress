/**
 * Copyright 2023 Dremio
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.support.diagnostics.stress;

import java.util.Locale;

/**
 * Defines the types of input files that can be used to generate queries for the stress tool. Each
 * enum constant represents a specific format or structure of the input file, such as a custom
 * stress JSON format or a simple list of queries.
 */
public enum QueriesGeneratorFileType {
  /** Represents a file in the stress tool's specific JSON format (`stress.json`). */
  STRESS_JSON,
  /** Represents a file containing a simple list of queries in JSON format (`queries.json`). */
  QUERIES_JSON;

  /**
   * Returns the string representation of the file type in uppercase.
   *
   * @return the file type name as an uppercase string.
   */
  @Override
  public String toString() {
    // Using name() is safer than ordinal() for string representation
    return name().toUpperCase(Locale.ROOT);
  }
}
