package io.w3security.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.w3security.jenkins.command.Command;
import io.w3security.jenkins.command.CommandLine;
import io.w3security.jenkins.config.W3SecurityConfig;
import io.w3security.jenkins.model.ObjectMapperHelper;
import io.w3security.jenkins.model.W3SecurityTestResult;
import io.w3security.jenkins.tools.W3SecurityInstallation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import static hudson.Util.fixEmptyAndTrim;
import static io.w3security.jenkins.Utils.getURLSafeDateTime;

public class W3SecurityTest {

  private static final Logger LOG = LoggerFactory.getLogger(W3SecurityTest.class);

  public static Result testProject(
      W3SecurityContext context,
      W3SecurityConfig config,
      W3SecurityInstallation installation,
      String w3securityToken) throws IOException, InterruptedException {
    PrintStream logger = context.getLogger();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    EnvVars envVars = context.getEnvVars();

    FilePath stdoutPath = workspace.child(getURLSafeDateTime() + "_w3security_report.json");

    ArgumentListBuilder command = CommandLine
        .asArgumentList(
            installation.getW3SecurityExecutable(launcher),
            Command.TEST,
            config,
            envVars);

    Map<String, String> commandEnvVars = CommandLine.asEnvVars(w3securityToken, envVars);

    int exitCode;
    try (OutputStream stdoutWriter = stdoutPath.write()) {
      logger.println("Testing project...");
      logger.println("> " + command);
      exitCode = launcher.launch()
          .cmds(command)
          .envs(commandEnvVars)
          .stdout(stdoutWriter)
          .stderr(logger)
          .quiet(true)
          .pwd(workspace)
          .join();
    }

    String stdout = stdoutPath.readToString();
    if (LOG.isTraceEnabled()) {
      LOG.trace("w3security test command: {}", command);
      LOG.trace("w3security test exit code: {}", exitCode);
      LOG.trace("w3security test stdout: {}", stdout);
    }

    W3SecurityTestResult result = ObjectMapperHelper.unmarshallTestResult(stdout);
    if (result == null) {
      throw new RuntimeException("Failed to parse test output.");
    }
    if (fixEmptyAndTrim(result.error) != null) {
      throw new RuntimeException("An error occurred. " + result.error);
    }
    if (exitCode >= 2) {
      throw new RuntimeException("An error occurred. (Exit Code: " + exitCode + ")");
    }
    if (!result.ok) {
      logger.println("Vulnerabilities found!");
      logger.printf(
          "Result: %s known vulnerabilities | %s dependencies%n",
          result.uniqueCount,
          result.dependencyCount);
    }

    return new Result(stdoutPath, exitCode);
  }

  public static class Result {
    public final FilePath testJsonPath;
    public final boolean foundIssues;

    public Result(FilePath testJsonPath, int exitCode) {
      this.testJsonPath = testJsonPath;
      this.foundIssues = exitCode == 1;
    }
  }

}
