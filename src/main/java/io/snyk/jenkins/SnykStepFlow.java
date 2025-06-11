package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.exception.SnykErrorException;
import io.snyk.jenkins.exception.SnykIssueException;
import io.snyk.jenkins.tools.SnykInstallation;

import java.io.IOException;
import java.util.function.Supplier;

public class SnykStepFlow {

  public static void perform(SnykConfig config, Supplier<SnykContext> contextSupplier)
  throws SnykIssueException, SnykErrorException {
    boolean foundIssues = false;
    Exception cause = null;
    SnykContext context = null;

    try {
      context = contextSupplier.get();

      SnykInstallation installation = SnykInstallation.install(
        context,
        config.getSnykInstallation()
      );

      String snykToken = SnykApiToken.getToken(context, config.getSnykTokenId());

      foundIssues = SnykStepFlow.testProject(context, config, installation, snykToken);

      if (config.isMonitorProjectOnBuild()) {
        SnykMonitor.monitorProject(context, config, installation, snykToken);
      }
    } catch (IOException | InterruptedException | RuntimeException ex) {
      if (context != null) {
        TaskListener listener = context.getTaskListener();
        if (ex instanceof IOException) {
          Util.displayIOException((IOException) ex, listener);
        }
        ex.printStackTrace(listener.fatalError("Snyk Security scan failed."));
      }
      cause = ex;
    }

    if (config.isFailOnIssues() && foundIssues) {
      throw new SnykIssueException();
    }
    if (config.isFailOnError() && cause != null) {
      throw new SnykErrorException(cause.getMessage());
    }
  }

  private static boolean testProject(SnykContext context, SnykConfig config, SnykInstallation installation, String snykToken)
  throws IOException, InterruptedException {
    SnykTest.Result testResult = SnykTest.testProject(context, config, installation, snykToken);
    FilePath report = SnykToHTML.generateReport(context, installation, testResult.testJsonPath);
    archiveReport(context, report);
    addSidebarLink(context);

    testResult.testJsonPath.delete();
    report.delete();

    return testResult.foundIssues;
  }

  private static void archiveReport(SnykContext context, FilePath report) throws IOException, InterruptedException {
    Run<?, ?> run = context.getRun();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    TaskListener listener = context.getTaskListener();
    EnvVars env = run.getEnvironment(listener);
    new ArtifactArchiver(report.getName())
      .perform(run, workspace, env, launcher, listener);
  }

  private static void addSidebarLink(SnykContext context) {
    Run<?, ?> run = context.getRun();
    if (run.getActions(SnykReportBuildAction.class).isEmpty()) {
      run.addAction(new SnykReportBuildAction());
    }
  }
}
