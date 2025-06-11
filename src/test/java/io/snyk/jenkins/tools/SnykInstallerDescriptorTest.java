package io.snyk.jenkins.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;

@WithJenkins
class SnykInstallerDescriptorTest {

  private SnykInstaller.SnykInstallerDescriptor descriptorUnderTest;

  private JenkinsRule jenkins;

  @BeforeEach
  void setUp(JenkinsRule rule) {
    jenkins = rule;
    descriptorUnderTest = new SnykInstaller.SnykInstallerDescriptor();
  }

  @Test
  void doFillPlatformItems_shouldReturnAllValuesFromPlatformItemEnum() {
    List<String> model = descriptorUnderTest.doFillPlatformItems().stream()
                                            .map(e -> e.value)
                                            .toList();

    assertThat(model.size(), is(5));
    assertThat(model, contains(PlatformItem.AUTO.name(),
                               PlatformItem.LINUX.name(),
                               PlatformItem.LINUX_ALPINE.name(),
                               PlatformItem.MAC_OS.name(),
                               PlatformItem.WINDOWS.name()));
  }
}
