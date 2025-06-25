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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class HttpApiResponseTest {

  @Test
  public void testDefaultState() {
    HttpApiResponse response = new HttpApiResponse();
    assertEquals(0, response.getResponseCode(), "Response code should be 0 by default");
    assertNull(response.getMessage(), "Message should be null by default");
    assertNull(response.getResponse(), "Response map should be null by default");
  }

  @Test
  public void testResponseCodeGetterAndSetter() {
    HttpApiResponse response = new HttpApiResponse();

    // Test setting and getting various response codes
    response.setResponseCode(200);
    assertEquals(200, response.getResponseCode(), "Response code should be 200");

    response.setResponseCode(404);
    assertEquals(404, response.getResponseCode(), "Response code should be 404");

    response.setResponseCode(500);
    assertEquals(500, response.getResponseCode(), "Response code should be 500");

    // Test edge cases
    response.setResponseCode(0);
    assertEquals(0, response.getResponseCode(), "Response code should be 0");

    response.setResponseCode(-1);
    assertEquals(-1, response.getResponseCode(), "Response code should be -1");
  }

  @Test
  public void testMessageGetterAndSetter() {
    HttpApiResponse response = new HttpApiResponse();

    // Test setting and getting message
    String message = "Success";
    response.setMessage(message);
    assertEquals(message, response.getMessage(), "Message should match the set value");
    assertSame(message, response.getMessage(), "Message reference should be the same");

    // Test setting empty message
    String emptyMessage = "";
    response.setMessage(emptyMessage);
    assertEquals(emptyMessage, response.getMessage(), "Empty message should be preserved");

    // Test setting null message
    response.setMessage(null);
    assertNull(response.getMessage(), "Message should be null after setting to null");

    // Test setting message with special characters
    String specialMessage = "Error: Invalid request with special chars: !@#$%^&*()";
    response.setMessage(specialMessage);
    assertEquals(
        specialMessage, response.getMessage(), "Special character message should be preserved");
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
    assertSame(responseMap, response.getResponse(), "Response map reference should be the same");
    assertEquals(
        responseMap.size(), response.getResponse().size(), "Response map size should match");
    assertEquals(responseMap, response.getResponse(), "Response map content should match");

    // Test individual map values
    assertEquals("success", response.getResponse().get("status"), "Status value should match");
    assertEquals("test data", response.getResponse().get("data"), "Data value should match");
    assertEquals(42, response.getResponse().get("count"), "Count value should match");

    // Test setting empty map
    Map<String, Object> emptyMap = new HashMap<>();
    response.setResponse(emptyMap);
    assertSame(emptyMap, response.getResponse(), "Empty map reference should be the same");
    assertEquals(0, response.getResponse().size(), "Empty map size should be 0");

    // Test setting null map
    response.setResponse(null);
    assertNull(response.getResponse(), "Response map should be null after setting to null");
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
    assertEquals(responseCode, response.getResponseCode(), "Response code should match");
    assertEquals(message, response.getMessage(), "Message should match");
    assertSame(responseMap, response.getResponse(), "Response map should match");
  }

  @Test
  public void testToString() {
    HttpApiResponse response = new HttpApiResponse();

    // Test toString with default values
    String defaultToString = response.toString();
    assertTrue(defaultToString.contains("HttpApiResponse"), "toString should contain class name");
    assertTrue(defaultToString.contains("responseCode=0"), "toString should contain responseCode");
    assertTrue(defaultToString.contains("message='null'"), "toString should contain message");
    assertTrue(defaultToString.contains("response=null"), "toString should contain response");

    // Test toString with all values set
    response.setResponseCode(200);
    response.setMessage("OK");
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("result", "success");
    response.setResponse(responseMap);

    String fullToString = response.toString();
    assertTrue(
        fullToString.contains("responseCode=200"), "toString should contain responseCode=200");
    assertTrue(fullToString.contains("message='OK'"), "toString should contain message='OK'");
    assertTrue(
        fullToString.contains("response={result=success}"), "toString should contain response map");
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
        toStringResult.contains("Error: 'Invalid' \"request\""),
        "toString should handle special characters in message");
    assertTrue(
        toStringResult.contains("Bad request with 'quotes' and \"double quotes\""),
        "toString should handle special characters in response map");
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
    assertEquals("test", response.getResponse().get("string"), "String value should be preserved");
    assertEquals(123, response.getResponse().get("integer"), "Integer value should be preserved");
    assertEquals(true, response.getResponse().get("boolean"), "Boolean value should be preserved");
    assertNull(response.getResponse().get("null_value"), "Null value should be preserved");

    @SuppressWarnings("unchecked")
    Map<String, Object> retrievedNestedMap =
        (Map<String, Object>) response.getResponse().get("nested");
    assertEquals(nestedMap, retrievedNestedMap, "Nested map should be preserved");
    assertEquals(
        "nested_value", retrievedNestedMap.get("nested_key"), "Nested value should be accessible");
  }
}
