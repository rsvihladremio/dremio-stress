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
package com.dremio.stress;

import com.dremio.support.diagnostics.stress.ConnectDremioApi;
import com.dremio.support.diagnostics.stress.StressExec;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ScopeType;



@CommandLine.Command(
    name = "stress",
    description =
        "using a defined JSON run a series of queries against dremio using various approaches",
    footer = """
            ### Example stress.json
            {
              "queries": [
                {
                  "query": "select * from  samples.\\"samples.dremio.com\\".\\"nyc-taxi-trips\\" where passenger_count = :count",
                  "frequency": 1,
                  "parameters": {
                    "count": [
                      1,
                      2,
                      3,
                      4
                    ]
                  }
                },
                {
                  "query": "select * FROM Samples.\\"samples.dremio.com\\".\\"SF weather 2018-2019.csv\\" where \\"DATE\\" between ':start' and ':end'",
                  "frequency": 1,
                  "parameters": {
                    "start": [
                      "2018-02-01"
                    ],
                    "end": [
                      "2018-02-10",
                      "2018-02-11",
                      "2018-02-12"
                    ]
                  }
                }
              ]
            }
            """,
    usageHelpWidth = 300,
    subcommands = CommandLine.HelpCommand.class)
public class DremioStress implements Callable<Integer> {

 public static void main(final String[] args) {
    //Locale.setDefault(Locale.US);
    final DremioStress app = new DremioStress();
    final String rawVersion = app.getVersion();
    final String version;
    if (rawVersion == null) {
      version = "DEV";
    } else {
      version = rawVersion;
    }
    System.out.println("stress version " + version); // NOPMD
    final int rc = new CommandLine(app).execute(args);
    System.exit(rc);
  }
  @CommandLine.Parameters(index = "0", description = "The file to use for stress definitions")
  private File yamlConfig;

  @CommandLine.Option(
      names = {"-q", "--max-queries-in-flight"},
      description = "max number of queries in flight (if possible)",
      defaultValue = "32")
  private Integer maxQueriesInFlight;

  @CommandLine.Option(
      names = {"-t", "--timeout-seconds"},
      description = "timeout for queries",
      defaultValue = "60")
  private Integer timeoutSeconds;

  @CommandLine.Option(
      names = {"-s", "--skip-ssl-verification"},
      description = "whether to skip ssl verification for queries or not",
      defaultValue = "false")
  private boolean skipSSLVerification;

  @CommandLine.Option(
      names = {"-d", "--duration-seconds"},
      description = "duration in seconds to run stress",
      defaultValue = "600")
  private Integer durationSeconds;

  /** http url for the rest api */
  @CommandLine.Option(
      names = {"-l", "--host"},
      description =
          "the http url of the dremio server which is used to submit sql and create spaces",
          defaultValue = "http://localhost:9047"
          )
  private String dremioHost;

  /** dremio user for the rest api */
  @CommandLine.Option(
      names = {"--user", "-u"},
      description = "the user used to submit sql and create spaces to the rest api")
  private String dremioUser;

  /** dremio password for the api user */
  @CommandLine.Option(
      names = {"--password", "-p"},
      interactive = false,
      description = "the password of the user used to submit sql and create spaces to the rest api")
  private String dremioPassword;

  private Package getPackage() {
    return this.getClass().getPackage();
  }
  private String getVersion() {
    return this.getPackage().getImplementationVersion();
  }
  /**
   * @return the exit code of the job 0 is success
   * @throws Exception when the job fails a general catch all exception
   */
  @Override
  public Integer call() throws Exception {
    final StressExec r =
        new StressExec(
            new ConnectDremioApi(),
            yamlConfig,
            dremioHost,
            dremioUser,
            dremioPassword,
            maxQueriesInFlight,
            timeoutSeconds,
            durationSeconds,
            skipSSLVerification);
    return r.run();
  }
}
