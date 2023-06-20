package io.w3security.jenkins.command;

import hudson.EnvVars;
import hudson.util.ArgumentListBuilder;
import io.w3security.jenkins.config.W3SecurityConfig;
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
    W3SecurityConfig config = Mockito.mock(W3SecurityConfig.class);

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/w3security", Command.MONITOR, config, env);

    assertThat(result.toList(), equalTo(asList("/usr/bin/w3security", "monitor")));
  }

  @Test
  public void shouldIncludeJsonForTest() {
    EnvVars env = new EnvVars();
    W3SecurityConfig config = Mockito.mock(W3SecurityConfig.class);

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/w3security", Command.TEST, config, env);

    assertThat(result.toList(), equalTo(asList("/usr/bin/w3security", "test", "--json")));
  }

  @Test
  public void shouldSetSeverityThreshold() {
    EnvVars env = new EnvVars();

    W3SecurityConfig config = Mockito.mock(W3SecurityConfig.class);
    when(config.getSeverity()).thenReturn("critical");

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/w3security", Command.TEST, config, env);

    assertThat(result.toList(), hasItem("--severity-threshold=critical"));
  }

  @Test
  public void shouldAppendMultipleArgumentsToTheEnd() {
    EnvVars env = new EnvVars();

    W3SecurityConfig config = Mockito.mock(W3SecurityConfig.class);
    when(config.getSeverity()).thenReturn("high");
    when(config.getProjectName()).thenReturn("my-project");

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/w3security", Command.TEST, config, env);

    assertThat(result.toList(), equalTo(asList(
        "/usr/bin/w3security",
        "test",
        "--json",
        "--severity-threshold=high",
        "--project-name=my-project")));
  }

  @Test
  public void shouldReplaceMacrosWithEnvironmentVariables() {
    EnvVars env = new EnvVars();
    env.put("BRANCH_NAME", "development");
    env.put("BUILD_NUMBER", "15");

    W3SecurityConfig config = Mockito.mock(W3SecurityConfig.class);
    when(config.getAdditionalArguments()).thenReturn("image:$BRANCH_NAME-${BUILD_NUMBER}");

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/w3security", Command.TEST, config, env);

    assertThat(result.toList(), hasItem("image:development-15"));
  }

  @Test
  public void shouldAppendAdditionalArgumentsToTheEnd() {
    EnvVars env = new EnvVars();

    W3SecurityConfig config = Mockito.mock(W3SecurityConfig.class);
    when(config.getAdditionalArguments()).thenReturn("--dev\n \n-d -- --settings=settings.xml");

    ArgumentListBuilder result = CommandLine.asArgumentList("/usr/bin/w3security", Command.TEST, config, env);

    assertThat(result.toList(), equalTo(asList(
        "/usr/bin/w3security",
        "test",
        "--json",
        "--dev",
        "-d",
        "--",
        "--settings=settings.xml")));
  }
}
