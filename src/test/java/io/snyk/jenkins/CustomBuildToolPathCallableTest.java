package io.snyk.jenkins;

import hudson.remoting.VirtualChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomBuildToolPathCallableTest {

  @Mock
  private File snykToolDirectory;
  @Mock
  private VirtualChannel channel;

  @Test
  void invoke_shouldNotThrownAnyExceptions_ifToolDirectoryNotContainsToolsWord() {
    when(snykToolDirectory.getAbsolutePath()).thenReturn("/path-without-t.o.o.l.s-word");
    CustomBuildToolPathCallable customBuildToolPath = new CustomBuildToolPathCallable();

    customBuildToolPath.invoke(snykToolDirectory, channel);
  }

  @Test
  void invoke_shouldNotThrownAnyExceptions_ifToolDirectoryContainsToolsWord() {
    when(snykToolDirectory.getAbsolutePath()).thenReturn("/opt/jenkins/tools/io.snyk.tools.SnykInstallation");
    CustomBuildToolPathCallable customBuildToolPath = new CustomBuildToolPathCallable();

    customBuildToolPath.invoke(snykToolDirectory, channel);
  }
}
