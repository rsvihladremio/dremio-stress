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

import static org.junit.Assert.*;

import org.junit.Test;

public class UsernamePasswordAuthTest {

  @Test
  public void testGetters() {
    UsernamePasswordAuth auth = new UsernamePasswordAuth("alice", "secret");
    assertEquals("getUsername should return the correct username", "alice", auth.getUsername());
    assertEquals("getPassword should return the correct password", "secret", auth.getPassword());
  }

  @Test
  public void testToString() {
    UsernamePasswordAuth auth = new UsernamePasswordAuth("bob", "hunter2");
    String expectedJson = "{\"userName\":\"bob\",\"password\":\"hunter2\"}";
    assertEquals("toString should return the correct JSON string", expectedJson, auth.toString());
  }
}
