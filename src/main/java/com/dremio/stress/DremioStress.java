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
package com.dremio.stress;

import static java.util.logging.Level.*;

import com.dremio.support.diagnostics.stress.ConnectDremioApi;
import com.dremio.support.diagnostics.stress.CustomLogFormatter;
import com.dremio.support.diagnostics.stress.Protocol;
import com.dremio.support.diagnostics.stress.QueriesGeneratorFileType;
import com.dremio.support.diagnostics.stress.QueriesSequence;
import com.dremio.support.diagnostics.stress.StressExec;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.logging.*;
import picocli.CommandLine;

@CommandLine.Command(
    name = "java -jar dremio-stress.jar",
    description =
        "using a defined JSON run a series of queries against dremio using various approaches",
    footer =
        "### Example stress.json\n"
            + "            {\n"
            + "              \"queries\": [\n"
            + "                {\n"
            + "                  \"query\": \"select * from "
            + " samples.\\\"samples.dremio.com\\\".\\\"nyc-taxi-trips\\\" where passenger_count ="
            + " :count\",\n"
            + "                  \"frequency\": 1,\n"
            + "                  \"parameters\": {\n"
            + "                    \"count\": [\n"
            + "                      1,\n"
            + "                      2,\n"
            + "                      3,\n"
            + "                      4\n"
            + "                    ]\n"
            + "                  }\n"
            + "                },\n"
            + "                {\n"
            + "                  \"query\": \"select * FROM"
            + " Samples.\\\"samples.dremio.com\\\".\\\"SF weather 2018-2019.csv\\\" where"
            + " \\\"DATE\\\" between ':start' and ':end'\",\n"
            + "                  \"frequency\": 1,\n"
            + "                  \"parameters\": {\n"
            + "                    \"start\": [\n"
            + "                      \"2018-02-01\"\n"
            + "                    ],\n"
            + "                    \"end\": [\n"
            + "                      \"2018-02-10\",\n"
            + "                      \"2018-02-11\",\n"
            + "                      \"2018-02-12\"\n"
            + "                    ]\n"
            + "                  }\n"
            + "                }\n"
            + "              ]\n"
            + "            }\n",
    usageHelpWidth = 300,
    subcommands = CommandLine.HelpCommand.class)
public class DremioStress implements Callable<Integer> {

  public static void main(final String[] args) {
    // Locale.setDefault(Locale.US);
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

  @CommandLine.Parameters(
      index = "0",
      description =
          "The file to use for query definitions. Supports queries.json.gz, queries.json, or a directory of queries.json and a stress.json file with a defined workload (see example)")
  private File jsonConfig;

  @CommandLine.Option(
      names = {"-q", "--max-queries-in-flight"},
      description = "max number of queries in flight (if possible)",
      defaultValue = "32")
  private Integer maxQueriesInFlight;

  @CommandLine.Option(
      names = {"-t", "--http-timeout-seconds"},
      description = "HTTP timeout for queries",
      defaultValue = "600")
  private Integer httpTimeoutSeconds;

  @CommandLine.Option(
      names = {"-s", "--http-skip-ssl-verification"},
      description = "whether to skip ssl verification for HTTP queries or not",
      defaultValue = "false")
  private boolean skipHttpSSLVerification;

  @CommandLine.Option(
      names = {"-d", "--duration-seconds"},
      description = "duration in seconds to run stress",
      defaultValue = "600")
  private Integer durationSeconds;

  /** protocol to use */
  @CommandLine.Option(
      names = {"--protocol"},
      description = "protocol to use HTTP, JDBC or LegacyJDBC",
      defaultValue = "HTTP")
  private Protocol protocol;

  /** http url or jdbc connection string */
  @CommandLine.Option(
      names = {"-l", "--url"},
      description = "JDBC connection string or HTTP url to connect")
  private String dremioUrl;

  /** dremio user for the rest api */
  @CommandLine.Option(
      names = {"--http-user", "-u"},
      description = "the user used to submit HTTP queries")
  private String dremioHttpUser;

  /** dremio password for the api user */
  @CommandLine.Option(
      names = {"--http-password", "-p"},
      interactive = false,
      description = "the password of the user used to submit HTTP queries")
  private String dremioHttpPassword;

  /** limit queries results to said limit */
  @CommandLine.Option(
      names = {"--limit-results"},
      interactive = false,
      description =
          "limit results to the specified number assuming there is not already a LIMIT in the query. This is an easy way to just add some limits on the result set size")
  private Integer limitResults;

  /** query generator file type aka what file type to use */
  @CommandLine.Option(
      names = {"--generator-type", "-g"},
      description = "specify QUERIES_JSON or STRESS_JSON to specify the engine type")
  private QueriesGeneratorFileType queriesGeneratorFileType;

  /** query execution sequence */
  @CommandLine.Option(
      names = {"--execution-sequence", "-x"},
      description = "specify RANDOM or SEQUENTIAL to specify the execution sequence",
      defaultValue = "RANDOM")
  private QueriesSequence queriesSequence;

  /** query index to restart from */
  @CommandLine.Option(
      names = {"--restart-index", "-i"},
      description = "specify query index to restart from (for SEQUENTIAL execution-sequence only)",
      defaultValue = "-1")
  private Integer queryIndexForRestart;

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
    final Logger root = Logger.getLogger("");
    setLogging(root);
    final StressExec r =
        new StressExec(
            new ConnectDremioApi(),
            jsonConfig,
            queriesGeneratorFileType,
            queriesSequence,
            queryIndexForRestart,
            limitResults,
            protocol,
            dremioUrl,
            dremioHttpUser,
            dremioHttpPassword,
            maxQueriesInFlight,
            httpTimeoutSeconds,
            durationSeconds,
            skipHttpSSLVerification);
    return r.run();
  }

  @CommandLine.Option( // W: Use explicit scoping instead of the default package private level
      names = {"-v", "--verbose"},
      description = "-v for info, -vv for debug, -vvv for trace")
  boolean[] verbose; // W: Fields should be declared at the top of the class, before any method

  // declarations, constructors, initializers or inner classes.

  void setLogging(
      final Logger
          root) { // W: To avoid mistakes add a comment at the beginning of the setLogging method if
    // you want a default access modifier
    final Level targetLevel = getTargetLevel();
    root.setLevel(targetLevel);
    for (final Handler handler : root.getHandlers()) {
      root.removeHandler(handler);
    }
    final CustomLogFormatter logFormatter = new CustomLogFormatter();
    final StreamHandler sh =
        new StreamHandler(System.out, logFormatter); // W: Avoid variables with short names like sh
    sh.setLevel(targetLevel);
    root.addHandler(sh);
    if (FINEST.equals(targetLevel)) {
      root.info("MAX logging enabled");
    } else if (FINER.equals(targetLevel)) {
      root.info("TRACE logging enabled");
    } else if (FINE.equals(targetLevel)) {
      root.info("DEBUG logging enabled");
    } else if (INFO.equals(targetLevel)) {
      root.info("INFO logging enabled");
    } else if (WARNING.equals(targetLevel)) {
      root.info("WARN logging enabled");
    } else if (SEVERE.equals(targetLevel)) {
      root.info("ERROR logging enabled");
    }
  }

  private static final int maxVerbosity = 3;
  private static final int traceVerbosity = 2;
  private static final int debubVerbosity = 1;

  private Level getTargetLevel() {
    final int verboseVs;
    if (verbose != null) {
      verboseVs = verbose.length;
    } else {
      return WARNING;
    }
    if (verboseVs > maxVerbosity) {
      return FINEST;
    } else if (verboseVs > traceVerbosity) {
      return FINER;
    } else if (verboseVs > debubVerbosity) {
      return FINE;
    }
    return INFO;
  }
}
