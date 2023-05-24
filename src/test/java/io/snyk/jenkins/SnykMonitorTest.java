package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.snyk.jenkins.tools.SnykInstallation;
import io.snyk.jenkins.workflow.SnykSecurityStep;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class SnykMonitorTest {
  @Mock
  private Launcher launchMock;
  @Mock
  SnykInstallation installationMock;
  @Mock
  Run<?, ?> buildMock;
  @Mock
  Launcher.ProcStarter starter;
  @Mock
  private TaskListener taskListenerMock;

  @SuppressWarnings("unchecked")
  @Before
  public void before() throws IOException, InterruptedException {
    MockitoAnnotations.openMocks(this);
    when(installationMock.getSnykExecutable(any())).thenReturn("snyk");
    when(launchMock.launch()).thenReturn(starter);
    when(starter.pwd(anyString())).thenReturn(starter);
    when(starter.cmds(anyString())).thenReturn(starter);
    when(starter.envs(any(Map.class))).thenReturn(starter);
    when(starter.stdout(any(OutputStream.class))).thenReturn(starter);
    when(starter.stderr(any())).thenReturn(starter);
    when(taskListenerMock.getLogger()).thenReturn(System.out);
    when(buildMock.getEnvironment(any())).thenReturn(new EnvVars());
  }

  @Test
  public void testMonitorShouldFailOnErrorIfConfigIsSet() throws IOException, InterruptedException {// mock setup
    when(starter.join()).thenReturn(2);

    SnykContext context = SnykContext.forFreestyleProject(buildMock, null, launchMock, taskListenerMock);
    SnykSecurityStep config = new SnykSecurityStep();
    config.setFailOnError(true);

    try (MockedStatic<PluginMetadata> pluginMetadataMockedStatic = Mockito.mockStatic(PluginMetadata.class)) {
      pluginMetadataMockedStatic.when(PluginMetadata::getIntegrationVersion).thenReturn("1.2.3");
      SnykMonitor.monitorProject(context, config, installationMock, "token");
      Assert.fail("Expected RuntimeException, but didn't get one");
    } catch (RuntimeException ignored) {
      // expected
    }
  }

  @Test
  public void testMonitorShouldNotFailOnErrorIfConfigIsNotSet() throws IOException, InterruptedException {// mock setup
    when(starter.join()).thenReturn(2);

    SnykContext context = SnykContext.forFreestyleProject(buildMock, null, launchMock, taskListenerMock);
    SnykSecurityStep config = new SnykSecurityStep();
    config.setFailOnError(false);

    try (MockedStatic<PluginMetadata> pluginMetadataMockedStatic = Mockito.mockStatic(PluginMetadata.class)) {
      pluginMetadataMockedStatic.when(PluginMetadata::getIntegrationVersion).thenReturn("1.2.3");
      SnykMonitor.monitorProject(context, config, installationMock, "token");
    }
  }

  @Test
  public void testMonitorShouldNotFailIfErrorCodeIs0AndConfigIsSet() throws IOException, InterruptedException {// mock setup
    when(starter.join()).thenReturn(0);

    SnykContext context = SnykContext.forFreestyleProject(buildMock, null, launchMock, taskListenerMock);
    SnykSecurityStep config = new SnykSecurityStep();
    config.setFailOnError(true);

    try (MockedStatic<PluginMetadata> pluginMetadataMockedStatic = Mockito.mockStatic(PluginMetadata.class)) {
      pluginMetadataMockedStatic.when(PluginMetadata::getIntegrationVersion).thenReturn("1.2.3");
      SnykMonitor.monitorProject(context, config, installationMock, "token");
    }
  }

  @Test
  public void testMonitorShouldNotFailIfErrorCodeIs0AndConfigIsNotSet() throws IOException, InterruptedException {// mock setup
    when(starter.join()).thenReturn(0);

    SnykContext context = SnykContext.forFreestyleProject(buildMock, null, launchMock, taskListenerMock);
    SnykSecurityStep config = new SnykSecurityStep();
    config.setFailOnError(false);

    try (MockedStatic<PluginMetadata> pluginMetadataMockedStatic = Mockito.mockStatic(PluginMetadata.class)) {
      pluginMetadataMockedStatic.when(PluginMetadata::getIntegrationVersion).thenReturn("1.2.3");
      SnykMonitor.monitorProject(context, config, installationMock, "token");
    }
  }
}

