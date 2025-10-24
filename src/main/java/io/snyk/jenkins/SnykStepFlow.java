package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import io.snyk.jenkins.command.Command;
import io.snyk.jenkins.command.CommandLine;
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
          config.getSnykInstallation());

      String snykToken = SnykApiToken.getToken(context, config.getSnykTokenId());

      String command = Util.fixEmptyAndTrim(config.getSnykCommand());
      if (command == null) {
        command = Command.TEST.getCommand();
      } else {
        CommandLine.validateCommand(command);
      }

      if (Command.TEST.getCommand().equals(command)) {
        foundIssues = SnykStepFlow.testProject(context, config, installation, snykToken);
      } else if (Command.SBOM_TEST.getCommand().equals(command)) {
        foundIssues = SnykStepFlow.testSbomProject(context, config, installation, snykToken);
      } else {
        SnykStepFlow.runCustomCommand(context, config, installation, snykToken, command);
      }

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

  private static boolean testSbomProject(SnykContext context, SnykConfig config, SnykInstallation installation,
      String snykToken)
      throws IOException, InterruptedException {
    SnykTest.Result testResult = SnykTest.testSbomProject(context, config, installation, snykToken);
    FilePath report = SnykToHTML.generateReport(context, installation, testResult.testJsonPath);
    archiveReport(context, report);
    addSidebarLink(context);

    testResult.testJsonPath.delete();
    report.delete();

    return testResult.foundIssues;
  }

  private static boolean testProject(SnykContext context, SnykConfig config, SnykInstallation installation,
      String snykToken)
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

  private static void runCustomCommand(SnykContext context, SnykConfig config, SnykInstallation installation,
      String snykToken, String command)
      throws IOException, InterruptedException {
    hudson.util.ArgumentListBuilder args = io.snyk.jenkins.command.CommandLine.asArgumentList(
        installation.getSnykExecutable(context.getLauncher()),
        command,
        config,
        context.getEnvVars());

    java.util.Map<String, String> commandEnvVars = io.snyk.jenkins.command.CommandLine.asEnvVars(snykToken,
        context.getEnvVars());

    java.io.PrintStream logger = context.getLogger();
    logger.println("Running Snyk command: " + command);
    logger.println("> " + args);

    int exitCode = context.getLauncher().launch()
        .cmds(args)
        .envs(commandEnvVars)
        .stdout(logger)
        .stderr(logger)
        .quiet(true)
        .pwd(context.getWorkspace())
        .join();

    if (exitCode >= 2) {
      throw new RuntimeException("An unexpected error occurred. (Exit Code: " + exitCode + ")");
    }
  }
}
