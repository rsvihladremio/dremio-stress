/**
 * Copyright 2022 Dremio
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

/** responsible for providing an interface to newly created directories and files */
public interface FileMaker {
  /**
   * generates a new directory for consumers. This is not guarenteed to be managed by the
   * implementer and if will be up to the consumer to remove or clean up any created directory
   * (unless it is managed by the operating system).
   *
   * <p>i.e. It is not safe to assume created directories will be cleaned up.
   *
   * @return path to newly created dir
   * @throws IOException due to being unable to create new dir
   */
  Path getNewDir() throws IOException;
}
