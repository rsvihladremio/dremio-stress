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
 * Defines the sequence in which queries should be executed by the stress tool. This determines
 * whether queries are picked randomly or in the order they appear.
 */
public enum QueriesSequence {
  /** Queries will be picked and executed in a random order. */
  RANDOM,
  /** Queries will be picked and executed in the order they are defined. */
  SEQUENTIAL;

  /**
   * Returns the string representation of the sequence type in uppercase.
   *
   * @return the sequence type name as an uppercase string.
   */
  @Override
  public String toString() {
    // Using name() is safer than ordinal() for string representation
    return name().toUpperCase(Locale.ROOT);
  }
}
