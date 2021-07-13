package io.snyk.jenkins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class SnykStepBuilderTestIT {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Test
  public void freeStyleProject_shouldFail_ifNoSnykInstallationExist() throws Exception {
    FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject("freestyle-project-without-snykInstallation");
    SnykStepBuilder snykStepBuilder = new SnykStepBuilder();
    snykStepBuilder.setSnykInstallation(null);
    freeStyleProject.getBuildersList().add(snykStepBuilder);

    FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();

    jenkins.assertBuildStatus(Result.FAILURE, build);
    jenkins.assertLogContains("Failed to install Snyk.", build);
  }
}
