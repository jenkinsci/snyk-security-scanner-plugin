package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.util.ArgumentListBuilder;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class SnykStepBuilderTest {

  @Test
  public void buildArgumentList_shouldReturnSeverityThresholdLow_ifSeverityNotDefined() {
    EnvVars env = new EnvVars();
    SnykStepBuilder snykStepBuilder = new SnykStepBuilder();

    ArgumentListBuilder resolvedArguments = snykStepBuilder.buildArgumentList("/usr/bin/snyk", "test", env);

    assertThat(resolvedArguments.toList(), hasItem("--severity-threshold=low"));
  }

  @Test
  public void buildArgumentList_shouldAppendConfigParametersAsSeparateArguments() {
    EnvVars env = new EnvVars();
    SnykStepBuilder snykStepBuilder = new SnykStepBuilder();
    snykStepBuilder.setProjectName("my-project");
    snykStepBuilder.setSeverity("high");

    ArgumentListBuilder resolvedArguments = snykStepBuilder.buildArgumentList("/usr/bin/snyk", "test", env);

    assertThat(resolvedArguments.toList(), hasSize(5));
    assertThat(resolvedArguments.toList(), contains("/usr/bin/snyk",
                                                    "test",
                                                    "--json",
                                                    "--severity-threshold=high",
                                                    "--project-name=my-project"));
  }

  @Test
  public void buildArgumentList_shouldResolveEnvVars() {
    EnvVars env = new EnvVars();
    env.put("BRANCH_NAME", "development");
    env.put("BUILD_NUMBER", "15");
    SnykStepBuilder snykStepBuilder = new SnykStepBuilder();
    snykStepBuilder.setAdditionalArguments("--docker image:$BRANCH_NAME-${BUILD_NUMBER}");

    ArgumentListBuilder resolvedArguments = snykStepBuilder.buildArgumentList("/usr/bin/snyk", "test", env);

    assertThat(resolvedArguments.toList(), hasSize(6));
    assertThat(resolvedArguments.toList(), contains("/usr/bin/snyk",
                                                    "test",
                                                    "--json",
                                                    "--severity-threshold=low",
                                                    "--docker",
                                                    "image:development-15"));
  }
}
