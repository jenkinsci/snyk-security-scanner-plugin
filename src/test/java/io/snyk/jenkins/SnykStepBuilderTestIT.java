package io.snyk.jenkins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SnykStepBuilderTestIT {

  private JenkinsRule jenkins;

  @BeforeEach
  void setUp(JenkinsRule rule) {
    jenkins = rule;
  }

  @Test
  void freeStyleProject_shouldFail_ifNoSnykInstallationExist() throws Exception {
    FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject("freestyle-project-without-snykInstallation");
    SnykStepBuilder snykStepBuilder = new SnykStepBuilder();
    snykStepBuilder.setSnykInstallation(null);
    freeStyleProject.getBuildersList().add(snykStepBuilder);

    FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();

    jenkins.assertBuildStatus(Result.FAILURE, build);
    jenkins.assertLogContains("Failed to install Snyk.", build);
  }
}
