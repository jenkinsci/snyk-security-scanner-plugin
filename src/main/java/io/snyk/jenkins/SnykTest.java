package io.snyk.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.command.Command;
import io.snyk.jenkins.command.CommandLine;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.model.ObjectMapperHelper;
import io.snyk.jenkins.model.SnykTestResult;
import io.snyk.jenkins.tools.SnykInstallation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static hudson.Util.fixEmptyAndTrim;
import static io.snyk.jenkins.config.SnykConstants.SNYK_TEST_REPORT_JSON;

public class SnykTest {

  private static final Logger LOG = LoggerFactory.getLogger(SnykTest.class);

  public static int testProject(
    SnykContext context,
    SnykConfig config,
    SnykInstallation installation
  ) throws IOException, InterruptedException {
    PrintStream logger = context.getLogger();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    EnvVars envVars = context.getEnvVars();

    int testExitCode;

    FilePath snykTestReport = workspace.child(SNYK_TEST_REPORT_JSON);
    FilePath snykTestDebug = workspace.child(SNYK_TEST_REPORT_JSON + ".debug");

    ArgumentListBuilder testCommand = CommandLine
      .asArgumentList(
        installation.getSnykExecutable(launcher),
        Command.TEST,
        config,
        envVars
      )
      .add("--json");

    try (
      OutputStream snykTestOutput = snykTestReport.write();
      OutputStream snykTestDebugOutput = snykTestDebug.write()
    ) {
      logger.println("Testing for known issues...");
      logger.println("> " + testCommand);
      testExitCode = launcher.launch()
        .cmds(testCommand)
        .envs(envVars)
        .stdout(snykTestOutput)
        .stderr(snykTestDebugOutput)
        .quiet(true)
        .pwd(workspace)
        .join();
    }

    String snykTestReportAsString = snykTestReport.readToString();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Command line arguments: {}", testCommand);
      LOG.trace("Exit code: {}", testExitCode);
      LOG.trace("Command standard output: {}", snykTestReportAsString);
      LOG.trace("Command debug output: {}", snykTestDebug.readToString());
    }

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(snykTestReportAsString);
    if (snykTestResult == null) {
      throw new AbortException("Could not parse generated json report file.");
    }
    // exit on cli error immediately
    if (fixEmptyAndTrim(snykTestResult.error) != null) {
      throw new AbortException("Error result: " + snykTestResult.error);
    }
    if (testExitCode >= 2) {
      throw new AbortException("An error occurred. Exit code is " + testExitCode);
    }
    if (!snykTestResult.ok) {
      logger.println("Vulnerabilities found!");
      logger.printf(
        "Result: %s known vulnerabilities | %s dependencies%n",
        snykTestResult.uniqueCount,
        snykTestResult.dependencyCount
      );
    }

    return testExitCode;
  }
}
