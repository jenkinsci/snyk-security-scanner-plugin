package io.snyk.jenkins;

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
  public static int perform(SnykContext context, SnykConfig config)
  throws IOException, InterruptedException {
    SnykInstallation installation = SnykInstallation.install(
      context,
      config.getSnykInstallation()
    );

    context.getEnvVars().put("SNYK_TOKEN", SnykApiToken.getToken(context, config.getSnykTokenId()));

    int testExitCode = SnykTest.testProject(context, config, installation);

    if (config.isMonitorProjectOnBuild()) {
      SnykMonitor.monitorProject(context, config, installation);
    }

    SnykToHTML.generateReport(context, installation);

    Run<?, ?> run = context.getRun();
    if (run.getActions(SnykReportBuildAction.class).isEmpty()) {
      run.addAction(new SnykReportBuildAction(run));
    }

    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    TaskListener listener = context.getTaskListener();
    ArtifactArchiver artifactArchiver = new ArtifactArchiver(workspace.getName() + "_" + SNYK_REPORT_HTML);
    artifactArchiver.perform(run, workspace, launcher, listener);

    return testExitCode;
  }
}
