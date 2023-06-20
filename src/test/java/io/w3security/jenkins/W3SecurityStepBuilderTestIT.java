package io.w3security.jenkins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class W3SecurityStepBuilderTestIT {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Test
  public void freeStyleProject_shouldFail_ifNoW3SecurityInstallationExist() throws Exception {
    FreeStyleProject freeStyleProject = jenkins.createFreeStyleProject("freestyle-project-without-w3securityInstallation");
    W3SecurityStepBuilder w3securityStepBuilder = new W3SecurityStepBuilder();
    w3securityStepBuilder.setW3SecurityInstallation(null);
    freeStyleProject.getBuildersList().add(w3securityStepBuilder);

    FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();

    jenkins.assertBuildStatus(Result.FAILURE, build);
    jenkins.assertLogContains("Failed to install W3Security.", build);
  }
}
