package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.command.CommandLine;
import io.snyk.jenkins.tools.SnykInstallation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.nio.charset.MalformedInputException;

import static io.snyk.jenkins.Utils.getURLSafeDateTime;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykToHTML {

  private static final Logger LOG = LoggerFactory.getLogger(SnykToHTML.class);

  public static FilePath generateReport(
    SnykContext context,
    SnykInstallation installation,
    FilePath testJsonPath
  ) {
    try {
      FilePath workspace = context.getWorkspace();
      Launcher launcher = context.getLauncher();
      EnvVars env = context.getEnvVars();
      PrintStream logger = context.getLogger();

      if (!testJsonPath.exists()) {
        throw new RuntimeException("Snyk Test JSON does not exist.");
      }

      FilePath stdoutPath = workspace.child(getURLSafeDateTime() + "_snyk_report.html");

      ArgumentListBuilder command = new ArgumentListBuilder()
        .add(installation.getReportExecutable(launcher))
        .add("-i", testJsonPath.getRemote());

      Map<String, String> commandEnvVars = CommandLine.asEnvVars(env);

      int exitCode;
      try (OutputStream reportWriter = stdoutPath.write()) {
        logger.println("Generating report...");
        logger.println("> " + command);
        exitCode = launcher.launch()
          .cmds(command)
          .envs(commandEnvVars)
          .stdout(reportWriter)
          .stderr(logger)
          .quiet(true)
          .pwd(workspace)
          .join();
      }

      if (exitCode != 0) {
        throw new RuntimeException("Exited with non-zero exit code. (Exit Code: " + exitCode + ")");
      }

      try {
        String stdout = stdoutPath.readToString();

        if (LOG.isTraceEnabled()) {
          LOG.trace("snyk-to-html command: {}", command);
          LOG.trace("snyk-to-html exit code: {}", exitCode);
          LOG.trace("snyk-to-html stdout: {}", stdout);
        }

        stdoutPath.write(stdout, UTF_8.name());
      } catch(MalformedInputException e) {
        logger.println("Couldn't convert report to UTF-8.");
      }

      return stdoutPath;
    } catch (IOException | InterruptedException | RuntimeException ex) {
      throw new RuntimeException("Failed to generate report.", ex);
    }
  }
}
