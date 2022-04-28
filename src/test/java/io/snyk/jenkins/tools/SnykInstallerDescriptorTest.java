package io.snyk.jenkins.tools;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

public class SnykInstallerDescriptorTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  private SnykInstaller.SnykInstallerDescriptor descriptorUnderTest;

  @Before
  public void setUp() {
    descriptorUnderTest = new SnykInstaller.SnykInstallerDescriptor();
  }

  @Test
  public void doFillPlatformItems_shouldReturnAllValuesFromPlatformItemEnum() {
    List<String> model = descriptorUnderTest.doFillPlatformItems().stream()
                                            .map(e -> e.value)
                                            .collect(Collectors.toList());

    assertThat(model.size(), is(5));
    assertThat(model, contains(PlatformItem.AUTO.name(),
                               PlatformItem.LINUX.name(),
                               PlatformItem.LINUX_ALPINE.name(),
                               PlatformItem.MAC_OS.name(),
                               PlatformItem.WINDOWS.name()));
  }
}
