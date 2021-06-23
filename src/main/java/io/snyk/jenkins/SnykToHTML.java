package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.transform.ReportConverter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;

import static io.snyk.jenkins.config.SnykConstants.SNYK_REPORT_HTML;
import static io.snyk.jenkins.config.SnykConstants.SNYK_TEST_REPORT_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykToHTML {
  public static void generateReport(
    Run<?, ?> build,
    @Nonnull FilePath workspace,
    Launcher launcher,
    TaskListener log,
    String reportExecutable,
    Optional<String> monitorUri
  ) throws IOException, InterruptedException {
    EnvVars env = build.getEnvironment(log);
    ArgumentListBuilder args = new ArgumentListBuilder();

    FilePath snykReportJson = workspace.child(SNYK_TEST_REPORT_JSON);
    if (!snykReportJson.exists()) {
      log.getLogger().println("Snyk report json doesn't exist");
      return;
    }

    workspace.child(SNYK_REPORT_HTML).write("", UTF_8.name());

    args.add(reportExecutable);
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
      ReportConverter reportConverter = ReportConverter.getInstance();
      String modifiedHtmlReport = reportConverter.modifyHeadSection(reportWithInlineCSS);
      String finalHtmlReport = monitorUri
        .map(uri -> reportConverter.injectMonitorLink(modifiedHtmlReport, uri))
        .orElse(modifiedHtmlReport);
      workspace.child(workspace.getName() + "_" + SNYK_REPORT_HTML).write(finalHtmlReport, UTF_8.name());
    } catch (IOException ex) {
      Util.displayIOException(ex, log);
      ex.printStackTrace(log.fatalError("Snyk-to-Html command execution failed"));
    }
  }
}
