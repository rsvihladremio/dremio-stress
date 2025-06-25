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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UsernamePasswordAuthTest {

  @Test
  public void testGetters() {
    UsernamePasswordAuth auth = new UsernamePasswordAuth("alice", "secret");
    assertEquals("alice", auth.getUsername(), "getUsername should return the correct username");
    assertEquals("secret", auth.getPassword(), "getPassword should return the correct password");
  }

  @Test
  public void testToString() {
    UsernamePasswordAuth auth = new UsernamePasswordAuth("bob", "hunter2");
    String expectedJson = "{\"userName\":\"bob\",\"password\":\"hunter2\"}";
    assertEquals(expectedJson, auth.toString(), "toString should return the correct JSON string");
  }
}
