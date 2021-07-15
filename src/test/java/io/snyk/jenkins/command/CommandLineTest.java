package io.snyk.jenkins.command;

import hudson.EnvVars;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.config.SnykConfig;
import org.junit.Test;
import org.mockito.Mockito;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;

public class CommandLineTest {

  @Test
  public void shouldIncludeCommand() {
    EnvVars env = new EnvVars();
    SnykConfig config = Mockito.mock(SnykConfig.class);

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/snyk", Command.TEST, config, env);

    assertThat(result.toList(), equalTo(asList("/usr/bin/snyk", "test")));
  }

  @Test
  public void shouldSetSeverityThreshold() {
    EnvVars env = new EnvVars();

    SnykConfig config = Mockito.mock(SnykConfig.class);
    when(config.getSeverity()).thenReturn("critical");

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/snyk", Command.TEST, config, env);

    assertThat(result.toList(), hasItem("--severity-threshold=critical"));
  }

  @Test
  public void shouldAppendMultipleArgumentsToTheEnd() {
    EnvVars env = new EnvVars();

    SnykConfig config = Mockito.mock(SnykConfig.class);
    when(config.getSeverity()).thenReturn("high");
    when(config.getProjectName()).thenReturn("my-project");

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/snyk", Command.TEST, config, env);

    assertThat(result.toList(), equalTo(asList(
      "/usr/bin/snyk",
      "test",
      "--severity-threshold=high",
      "--project-name=my-project"
    )));
  }

  @Test
  public void shouldReplaceMacrosWithEnvironmentVariables() {
    EnvVars env = new EnvVars();
    env.put("BRANCH_NAME", "development");
    env.put("BUILD_NUMBER", "15");

    SnykConfig config = Mockito.mock(SnykConfig.class);
    when(config.getAdditionalArguments()).thenReturn("image:$BRANCH_NAME-${BUILD_NUMBER}");

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/snyk", Command.TEST, config, env);

    assertThat(result.toList(), hasItem("image:development-15"));
  }

  @Test
  public void shouldIncludeAdditionalArguments() {
    EnvVars env = new EnvVars();

    SnykConfig config = Mockito.mock(SnykConfig.class);
    when(config.getAdditionalArguments()).thenReturn("--dev\n \n-d");

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/snyk", Command.TEST, config, env);

    assertThat(result.toList(), equalTo(asList(
      "/usr/bin/snyk",
      "test",
      "--dev",
      "-d"
    )));
  }
}
