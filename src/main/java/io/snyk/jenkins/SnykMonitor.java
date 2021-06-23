package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.model.ObjectMapperHelper;
import io.snyk.jenkins.model.SnykMonitorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static hudson.Util.fixEmptyAndTrim;
import static io.snyk.jenkins.CLIArgs.buildArgumentList;
import static io.snyk.jenkins.config.SnykConstants.SNYK_MONITOR_REPORT_JSON;

public class SnykMonitor {

  private static final Logger LOG = LoggerFactory.getLogger(SnykMonitor.class.getName());

  public static Optional<String> execute(
    FilePath workspace,
    Launcher launcher,
    EnvVars envVars,
    PrintStream logger,
    String snykExecutable,
    Severity severity,
    String targetFile,
    String organisation,
    String projectName,
    String additionalArguments
  )
  throws IOException, InterruptedException {
    FilePath snykMonitorReport = workspace.child(SNYK_MONITOR_REPORT_JSON);
    FilePath snykMonitorDebug = workspace.child(SNYK_MONITOR_REPORT_JSON + ".debug");

    ArgumentListBuilder argsForMonitorCommand = buildArgumentList(
      snykExecutable,
      "monitor",
      envVars,
      severity,
      targetFile,
      organisation,
      projectName,
      additionalArguments
    );

    logger.println("Remember project for continuous monitoring...");
    logger.println("> " + argsForMonitorCommand);

    int monitorExitCode;
    try (
      OutputStream snykMonitorOutput = snykMonitorReport.write();
      OutputStream snykMonitorDebugOutput = snykMonitorDebug.write()
    ) {
      monitorExitCode = launcher.launch()
        .cmds(argsForMonitorCommand)
        .envs(envVars)
        .stdout(snykMonitorOutput)
        .stderr(snykMonitorDebugOutput)
        .quiet(true)
        .pwd(workspace)
        .join();
    }
    String snykMonitorReportAsString = snykMonitorReport.readToString();
    if (monitorExitCode != 0) {
      logger.println("Warning: 'snyk monitor' was not successful. Exit code: " + monitorExitCode);
      logger.println(snykMonitorReportAsString);
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("Command line arguments: {}", argsForMonitorCommand);
      LOG.trace("Exit code: {}", monitorExitCode);
      LOG.trace("Command standard output: {}", snykMonitorReportAsString);
      LOG.trace("Command debug output: {}", snykMonitorDebug.readToString());
    }

    SnykMonitorResult snykMonitorResult = ObjectMapperHelper.unmarshallMonitorResult(snykMonitorReportAsString);
    if (snykMonitorResult != null && fixEmptyAndTrim(snykMonitorResult.uri) != null) {
      logger.println("Explore the snapshot at " + snykMonitorResult.uri);
      return Optional.of(snykMonitorResult.uri);
    }
    return Optional.empty();
  }
}
