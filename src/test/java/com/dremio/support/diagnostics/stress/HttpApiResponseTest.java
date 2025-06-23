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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class HttpApiResponseTest {

  @Test
  public void testDefaultState() {
    HttpApiResponse response = new HttpApiResponse();
    assertEquals("Response code should be 0 by default", 0, response.getResponseCode());
    assertNull("Message should be null by default", response.getMessage());
    assertNull("Response map should be null by default", response.getResponse());
  }

  @Test
  public void testResponseCodeGetterAndSetter() {
    HttpApiResponse response = new HttpApiResponse();

    // Test setting and getting various response codes
    response.setResponseCode(200);
    assertEquals("Response code should be 200", 200, response.getResponseCode());

    response.setResponseCode(404);
    assertEquals("Response code should be 404", 404, response.getResponseCode());

    response.setResponseCode(500);
    assertEquals("Response code should be 500", 500, response.getResponseCode());

    // Test edge cases
    response.setResponseCode(0);
    assertEquals("Response code should be 0", 0, response.getResponseCode());

    response.setResponseCode(-1);
    assertEquals("Response code should be -1", -1, response.getResponseCode());
  }

  @Test
  public void testMessageGetterAndSetter() {
    HttpApiResponse response = new HttpApiResponse();

    // Test setting and getting message
    String message = "Success";
    response.setMessage(message);
    assertEquals("Message should match the set value", message, response.getMessage());
    assertSame("Message reference should be the same", message, response.getMessage());

    // Test setting empty message
    String emptyMessage = "";
    response.setMessage(emptyMessage);
    assertEquals("Empty message should be preserved", emptyMessage, response.getMessage());

    // Test setting null message
    response.setMessage(null);
    assertNull("Message should be null after setting to null", response.getMessage());

    // Test setting message with special characters
    String specialMessage = "Error: Invalid request with special chars: !@#$%^&*()";
    response.setMessage(specialMessage);
    assertEquals(
        "Special character message should be preserved", specialMessage, response.getMessage());
  }

  @Test
  public void testResponseMapGetterAndSetter() {
    HttpApiResponse response = new HttpApiResponse();

    // Test setting and getting response map
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("status", "success");
    responseMap.put("data", "test data");
    responseMap.put("count", 42);

    response.setResponse(responseMap);
    assertSame("Response map reference should be the same", responseMap, response.getResponse());
    assertEquals(
        "Response map size should match", responseMap.size(), response.getResponse().size());
    assertEquals("Response map content should match", responseMap, response.getResponse());

    // Test individual map values
    assertEquals("Status value should match", "success", response.getResponse().get("status"));
    assertEquals("Data value should match", "test data", response.getResponse().get("data"));
    assertEquals("Count value should match", 42, response.getResponse().get("count"));

    // Test setting empty map
    Map<String, Object> emptyMap = new HashMap<>();
    response.setResponse(emptyMap);
    assertSame("Empty map reference should be the same", emptyMap, response.getResponse());
    assertEquals("Empty map size should be 0", 0, response.getResponse().size());

    // Test setting null map
    response.setResponse(null);
    assertNull("Response map should be null after setting to null", response.getResponse());
  }

  @Test
  public void testCompleteObjectState() {
    HttpApiResponse response = new HttpApiResponse();

    // Set all properties
    int responseCode = 201;
    String message = "Created successfully";
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("id", "12345");
    responseMap.put("created", true);

    response.setResponseCode(responseCode);
    response.setMessage(message);
    response.setResponse(responseMap);

    // Verify all properties are set correctly
    assertEquals("Response code should match", responseCode, response.getResponseCode());
    assertEquals("Message should match", message, response.getMessage());
    assertSame("Response map should match", responseMap, response.getResponse());
  }

  @Test
  public void testToString() {
    HttpApiResponse response = new HttpApiResponse();

    // Test toString with default values
    String defaultToString = response.toString();
    assertTrue("toString should contain class name", defaultToString.contains("HttpApiResponse"));
    assertTrue("toString should contain responseCode", defaultToString.contains("responseCode=0"));
    assertTrue("toString should contain message", defaultToString.contains("message='null'"));
    assertTrue("toString should contain response", defaultToString.contains("response=null"));

    // Test toString with all values set
    response.setResponseCode(200);
    response.setMessage("OK");
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("result", "success");
    response.setResponse(responseMap);

    String fullToString = response.toString();
    assertTrue(
        "toString should contain responseCode=200", fullToString.contains("responseCode=200"));
    assertTrue("toString should contain message='OK'", fullToString.contains("message='OK'"));
    assertTrue(
        "toString should contain response map", fullToString.contains("response={result=success}"));
  }

  @Test
  public void testToStringWithSpecialCharacters() {
    HttpApiResponse response = new HttpApiResponse();
    response.setResponseCode(400);
    response.setMessage("Error: 'Invalid' \"request\"");

    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("error", "Bad request with 'quotes' and \"double quotes\"");
    response.setResponse(responseMap);

    String toStringResult = response.toString();
    assertTrue(
        "toString should handle special characters in message",
        toStringResult.contains("Error: 'Invalid' \"request\""));
    assertTrue(
        "toString should handle special characters in response map",
        toStringResult.contains("Bad request with 'quotes' and \"double quotes\""));
  }

  @Test
  public void testResponseMapWithComplexObjects() {
    HttpApiResponse response = new HttpApiResponse();

    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("string", "test");
    responseMap.put("integer", 123);
    responseMap.put("boolean", true);
    responseMap.put("null_value", null);

    // Add nested map
    Map<String, Object> nestedMap = new HashMap<>();
    nestedMap.put("nested_key", "nested_value");
    responseMap.put("nested", nestedMap);

    response.setResponse(responseMap);

    // Verify complex objects are preserved
    assertEquals("String value should be preserved", "test", response.getResponse().get("string"));
    assertEquals("Integer value should be preserved", 123, response.getResponse().get("integer"));
    assertEquals("Boolean value should be preserved", true, response.getResponse().get("boolean"));
    assertNull("Null value should be preserved", response.getResponse().get("null_value"));

    @SuppressWarnings("unchecked")
    Map<String, Object> retrievedNestedMap =
        (Map<String, Object>) response.getResponse().get("nested");
    assertEquals("Nested map should be preserved", nestedMap, retrievedNestedMap);
    assertEquals(
        "Nested value should be accessible", "nested_value", retrievedNestedMap.get("nested_key"));
  }
}
