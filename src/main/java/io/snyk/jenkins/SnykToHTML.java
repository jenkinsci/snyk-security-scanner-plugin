package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.tools.SnykInstallation;
import io.snyk.jenkins.transform.ReportConverter;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Instant;

import static io.snyk.jenkins.config.SnykConstants.SNYK_REPORT_HTML;
import static io.snyk.jenkins.config.SnykConstants.SNYK_TEST_REPORT_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykToHTML {

  private static final Logger LOG = LoggerFactory.getLogger(SnykToHTML.class);

  public static FilePath generateReport(
    SnykContext context,
    SnykInstallation installation
  ) {
    try {
      FilePath workspace = context.getWorkspace();
      Launcher launcher = context.getLauncher();
      EnvVars env = context.getEnvVars();
      PrintStream logger = context.getLogger();

      FilePath snykReportJson = workspace.child(SNYK_TEST_REPORT_JSON);
      if (!snykReportJson.exists()) {
        throw new RuntimeException("Snyk Report JSON does not exist.");
      }

      FilePath stdoutPath = workspace.child(getURLSafeDateTime() + "_" + SNYK_REPORT_HTML);
      stdoutPath.write("", UTF_8.name());

      ArgumentListBuilder command = new ArgumentListBuilder()
        .add(installation.getReportExecutable(launcher))
        .add("-i", SNYK_TEST_REPORT_JSON);

      int exitCode;
      try (OutputStream reportWriter = stdoutPath.write()) {
        exitCode = launcher.launch()
          .cmds(command)
          .envs(env)
          .stdout(reportWriter)
          .stderr(logger)
          .quiet(true)
          .pwd(workspace)
          .join();
      }

      String stdout = stdoutPath.readToString();

      if (LOG.isTraceEnabled()) {
        LOG.trace("snyk-to-html command: {}", command);
        LOG.trace("snyk-to-html exit code: {}", exitCode);
        LOG.trace("snyk-to-html stdout: {}", stdout);
      }

      if (exitCode != 0) {
        throw new RuntimeException("Exited with non-zero exit code. (Exit Code: " + exitCode + ")");
      }

      String reportContents = ReportConverter.getInstance().modifyHeadSection(
        stdout,
        Jenkins.get().servletContext.getContextPath()
      );

      stdoutPath.write(reportContents, UTF_8.name());

      return stdoutPath;
    } catch (IOException | InterruptedException | RuntimeException ex) {
      throw new RuntimeException("Failed to generate report.", ex);
    }
  }

  private static String getURLSafeDateTime() {
    return Instant.now().toString()
      .replaceAll(":", "-")
      .replaceAll("\\.", "-");
  }
}
