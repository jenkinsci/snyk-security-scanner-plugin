package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.snyk.jenkins.tools.SnykInstallation;
import io.snyk.jenkins.workflow.SnykSecurityStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SnykMonitorTest {

  @Mock
  private Launcher launchMock;
  @Mock
  private SnykInstallation installationMock;
  @Mock
  private Run<?, ?> buildMock;
  @Mock
  private Launcher.ProcStarter starter;
  @Mock
  private TaskListener taskListenerMock;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() throws IOException, InterruptedException {
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
  void testMonitorShouldFailOnErrorIfConfigIsSet() throws IOException, InterruptedException {// mock setup
    when(starter.join()).thenReturn(2);

    SnykContext context = SnykContext.forFreestyleProject(buildMock, null, launchMock, taskListenerMock);
    SnykSecurityStep config = new SnykSecurityStep();
    config.setFailOnError(true);

    try (MockedStatic<PluginMetadata> pluginMetadataMockedStatic = Mockito.mockStatic(PluginMetadata.class)) {
      pluginMetadataMockedStatic.when(PluginMetadata::getIntegrationVersion).thenReturn("1.2.3");
      assertThrows(RuntimeException.class, () -> SnykMonitor.monitorProject(context, config, installationMock, "token"));
    }
  }

  @Test
  void testMonitorShouldNotFailOnErrorIfConfigIsNotSet() throws IOException, InterruptedException {// mock setup
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
  void testMonitorShouldNotFailIfErrorCodeIs0AndConfigIsSet() throws IOException, InterruptedException {// mock setup
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
  void testMonitorShouldNotFailIfErrorCodeIs0AndConfigIsNotSet() throws IOException, InterruptedException {// mock setup
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

