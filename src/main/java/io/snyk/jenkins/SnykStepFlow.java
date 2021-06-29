package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.tools.SnykInstallation;

import java.io.IOException;

import static io.snyk.jenkins.config.SnykConstants.SNYK_REPORT_HTML;

public class SnykStepFlow {
  public static int perform(
    SnykConfig config,
    FilePath workspace,
    EnvVars envVars,
    TaskListener listener,
    Run<?, ?> run,
    Launcher launcher
  )
  throws IOException, InterruptedException {
    SnykInstallation installation = SnykInstallation.install(
      config.getSnykInstallation(),
      workspace,
      envVars,
      listener
    );

    envVars.put("SNYK_TOKEN", SnykApiToken.getToken(config.getSnykTokenId(), run));

    int testExitCode = SnykTest.testProject(workspace, launcher, installation, config, envVars, listener);

    if (config.isMonitorProjectOnBuild()) {
      SnykMonitor.monitorProject(workspace, launcher, installation, config, envVars, listener);
    }

    SnykToHTML.generateReport(run, workspace, launcher, installation, listener);

    if (run.getActions(SnykReportBuildAction.class).isEmpty()) {
      run.addAction(new SnykReportBuildAction(run));
    }

    ArtifactArchiver artifactArchiver = new ArtifactArchiver(workspace.getName() + "_" + SNYK_REPORT_HTML);
    artifactArchiver.perform(run, workspace, launcher, listener);

    return testExitCode;
  }
}
