package io.w3security.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.w3security.jenkins.command.CommandLine;
import io.w3security.jenkins.tools.W3SecurityInstallation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import static io.w3security.jenkins.Utils.getURLSafeDateTime;
import static java.nio.charset.StandardCharsets.UTF_8;

public class W3SecurityToHTML {

  private static final Logger LOG = LoggerFactory.getLogger(W3SecurityToHTML.class);

  public static FilePath generateReport(
      W3SecurityContext context,
      W3SecurityInstallation installation,
      FilePath testJsonPath) {
    try {
      FilePath workspace = context.getWorkspace();
      Launcher launcher = context.getLauncher();
      EnvVars env = context.getEnvVars();
      PrintStream logger = context.getLogger();

      if (!testJsonPath.exists()) {
        throw new RuntimeException("W3Security Test JSON does not exist.");
      }

      FilePath stdoutPath = workspace.child(getURLSafeDateTime() + "_w3security_report.html");

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

      String stdout = stdoutPath.readToString();

      if (LOG.isTraceEnabled()) {
        LOG.trace("w3security-to-html command: {}", command);
        LOG.trace("w3security-to-html exit code: {}", exitCode);
        LOG.trace("w3security-to-html stdout: {}", stdout);
      }

      if (exitCode != 0) {
        throw new RuntimeException("Exited with non-zero exit code. (Exit Code: " + exitCode + ")");
      }

      stdoutPath.write(stdout, UTF_8.name());

      return stdoutPath;
    } catch (IOException | InterruptedException | RuntimeException ex) {
      throw new RuntimeException("Failed to generate report.", ex);
    }
  }
}
