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

import java.util.Objects;

/**
 * Represents a response from a Dremio API call. This class encapsulates the success status and any
 * error message that may have been returned from the API.
 */
public class DremioApiResponse {
  /** Error message if the operation failed. */
  private String errorMessage;

  /** Flag indicating whether the operation was successful. */
  private boolean created;

  /**
   * Sets the error message on the response.
   *
   * @param errorMessage Error message that occurred during this operation
   */
  public void setErrorMessage(final String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   * Sets the success status of the operation.
   *
   * @param created Boolean indicating whether the operation was successful
   */
  public void setSuccessful(final boolean created) {
    this.created = created;
  }

  /**
   * Gets the success status of the operation.
   *
   * @return True if the operation was successful, false otherwise
   */
  public boolean isSuccessful() {
    return created;
  }

  /**
   * Gets the error message if the operation failed.
   *
   * @return The error message, or null if no error occurred
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Compares this DremioApiResponse with another object for equality. Two DremioApiResponse objects
   * are considered equal if they have the same success status and error message.
   *
   * @param o The object to compare with
   * @return True if the objects are equal, false otherwise
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DremioApiResponse)) return false;
    DremioApiResponse that = (DremioApiResponse) o;
    return created == that.created && Objects.equals(errorMessage, that.errorMessage);
  }

  /**
   * Generates a hash code for this DremioApiResponse.
   *
   * @return The hash code
   */
  @Override
  public int hashCode() {
    return Objects.hash(errorMessage, created);
  }
}
