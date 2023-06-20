package io.w3security.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import io.w3security.jenkins.config.W3SecurityConfig;
import io.w3security.jenkins.credentials.W3SecurityApiToken;
import io.w3security.jenkins.exception.W3SecurityErrorException;
import io.w3security.jenkins.exception.W3SecurityIssueException;
import io.w3security.jenkins.tools.W3SecurityInstallation;

import java.io.IOException;
import java.util.function.Supplier;

public class W3SecurityStepFlow {

  public static void perform(W3SecurityConfig config, Supplier<W3SecurityContext> contextSupplier)
      throws W3SecurityIssueException, W3SecurityErrorException {
    boolean foundIssues = false;
    Exception cause = null;
    W3SecurityContext context = null;

    try {
      context = contextSupplier.get();

      W3SecurityInstallation installation = W3SecurityInstallation.install(
          context,
          config.getW3SecurityInstallation());

      String w3securityToken = W3SecurityApiToken.getToken(context, config.getW3SecurityTokenId());

      foundIssues = W3SecurityStepFlow.testProject(context, config, installation, w3securityToken);

      if (config.isMonitorProjectOnBuild()) {
        W3SecurityMonitor.monitorProject(context, config, installation, w3securityToken);
      }
    } catch (IOException | InterruptedException | RuntimeException ex) {
      if (context != null) {
        TaskListener listener = context.getTaskListener();
        if (ex instanceof IOException) {
          Util.displayIOException((IOException) ex, listener);
        }
        ex.printStackTrace(listener.fatalError("W3Security Security scan failed."));
      }
      cause = ex;
    }

    if (config.isFailOnIssues() && foundIssues) {
      throw new W3SecurityIssueException();
    }
    if (config.isFailOnError() && cause != null) {
      throw new W3SecurityErrorException(cause.getMessage());
    }
  }

  private static boolean testProject(W3SecurityContext context, W3SecurityConfig config, W3SecurityInstallation installation,
      String w3securityToken)
      throws IOException, InterruptedException {
    W3SecurityTest.Result testResult = W3SecurityTest.testProject(context, config, installation, w3securityToken);
    FilePath report = W3SecurityToHTML.generateReport(context, installation, testResult.testJsonPath);
    archiveReport(context, report);
    addSidebarLink(context);

    testResult.testJsonPath.delete();
    report.delete();

    return testResult.foundIssues;
  }

  private static void archiveReport(W3SecurityContext context, FilePath report) throws IOException, InterruptedException {
    Run<?, ?> run = context.getRun();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    TaskListener listener = context.getTaskListener();
    new ArtifactArchiver(report.getName())
        .perform(run, workspace, launcher, listener);
  }

  private static void addSidebarLink(W3SecurityContext context) {
    Run<?, ?> run = context.getRun();
    if (run.getActions(W3SecurityReportBuildAction.class).isEmpty()) {
      run.addAction(new W3SecurityReportBuildAction());
    }
  }
}
