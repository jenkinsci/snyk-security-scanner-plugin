package io.snyk.jenkins;

import java.io.File;

import hudson.remoting.VirtualChannel;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.when;

public class CustomBuildToolPathCallableTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock
  private File snykToolDirectory;
  @Mock
  private VirtualChannel channel;

  @Test
  public void invoke_shouldNotThrownAnyExceptions_ifToolDirectoryNotContainsToolsWord() {
    when(snykToolDirectory.getAbsolutePath()).thenReturn("/path-without-t.o.o.l.s-word");
    CustomBuildToolPathCallable customBuildToolPath = new CustomBuildToolPathCallable();

    customBuildToolPath.invoke(snykToolDirectory, channel);
  }

  @Test
  public void invoke_shouldNotThrownAnyExceptions_ifToolDirectoryContainsToolsWord() {
    when(snykToolDirectory.getAbsolutePath()).thenReturn("/opt/jenkins/tools/io.snyk.tools.SnykInstallation");
    CustomBuildToolPathCallable customBuildToolPath = new CustomBuildToolPathCallable();

    customBuildToolPath.invoke(snykToolDirectory, channel);
  }
}
