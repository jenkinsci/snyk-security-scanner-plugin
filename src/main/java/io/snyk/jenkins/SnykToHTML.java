package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.tools.SnykInstallation;
import io.snyk.jenkins.transform.ReportConverter;
import jenkins.model.Jenkins;

import java.io.IOException;

import static io.snyk.jenkins.config.SnykConstants.SNYK_REPORT_HTML;
import static io.snyk.jenkins.config.SnykConstants.SNYK_TEST_REPORT_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykToHTML {
  public static void generateReport(
    SnykContext context,
    SnykInstallation installation
  ) throws IOException, InterruptedException {
    TaskListener log = context.getTaskListener();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    EnvVars env = context.getEnvVars();

    ArgumentListBuilder args = new ArgumentListBuilder();

    FilePath snykReportJson = workspace.child(SNYK_TEST_REPORT_JSON);
    if (!snykReportJson.exists()) {
      log.getLogger().println("Snyk report json doesn't exist");
      return;
    }

    workspace.child(SNYK_REPORT_HTML).write("", UTF_8.name());

    args.add(installation.getReportExecutable(launcher));
    args.add("-i", SNYK_TEST_REPORT_JSON, "-o", SNYK_REPORT_HTML);
    try {
      int exitCode = launcher.launch()
        .cmds(args)
        .envs(env)
        .quiet(true)
        .pwd(workspace)
        .join();
      boolean success = exitCode == 0;
      if (!success) {
        log.getLogger().println("Generating Snyk html report was not successful");
      }
      String reportWithInlineCSS = workspace.child(SNYK_REPORT_HTML).readToString();
      String finalHtmlReport = ReportConverter.getInstance().modifyHeadSection(
        reportWithInlineCSS,
        Jenkins.get().servletContext.getContextPath()
      );
      workspace.child(workspace.getName() + "_" + SNYK_REPORT_HTML).write(finalHtmlReport, UTF_8.name());
    } catch (IOException ex) {
      Util.displayIOException(ex, log);
      ex.printStackTrace(log.fatalError("Snyk-to-Html command execution failed"));
    }
  }
}
