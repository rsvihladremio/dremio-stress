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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface responsible for providing access to newly created directories and files. This
 * abstraction allows for different implementations of file system operations, which can be useful
 * for testing or supporting different storage backends.
 */
public interface FileMaker {
  /**
   * Generates a new directory for consumers.
   *
   * <p>Note: This is not guaranteed to be managed by the implementer. It will be up to the consumer
   * to remove or clean up any created directory (unless it is managed by the operating system).
   *
   * <p>i.e. It is not safe to assume created directories will be cleaned up automatically.
   *
   * @return Path to the newly created directory
   * @throws IOException If unable to create the new directory
   */
  Path getNewDir() throws IOException;
}
