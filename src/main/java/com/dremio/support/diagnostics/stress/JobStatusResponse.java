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

/**
 * Represents the response received when checking the status of a job via an API. This class
 * typically contains the job's current status and an optional message.
 */
public class JobStatusResponse {

  /**
   * Gets the message associated with the job status response. This might contain details about the
   * status or any errors.
   *
   * @return the message string.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message associated with the job status response.
   *
   * @param message the message string to set.
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Gets the status of the job. This usually indicates the current state (e.g., RUNNING, COMPLETED,
   * FAILED).
   *
   * @return the status string.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the status of the job.
   *
   * @param status the status string to set.
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /** An optional message providing details about the job status. */
  private String message;
  /** The current status of the job (e.g., RUNNING, COMPLETED). */
  private String status;
}
