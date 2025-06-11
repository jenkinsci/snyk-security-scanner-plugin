package io.snyk.jenkins.jcasc;

import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.snyk.jenkins.tools.PlatformItem;
import io.snyk.jenkins.tools.SnykInstallation;
import io.snyk.jenkins.tools.SnykInstaller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

  @Test
  @ConfiguredWithCode("configuration-as-code.yml")
  void should_support_configuration_as_code(JenkinsConfiguredWithCodeRule r) {
    SnykInstallation.SnykInstallationDescriptor descriptor = ExtensionList.lookupSingleton(SnykInstallation.SnykInstallationDescriptor.class);
    assertEquals(1, descriptor.getInstallations().length);

    SnykInstallation installation = descriptor.getInstallations()[0];
    assertEquals("snyk", installation.getName());
    DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = installation.getProperties();
    DescribableList<ToolInstaller, Descriptor<ToolInstaller>> installers = ((InstallSourceProperty) properties.get(0)).installers;
    assertEquals(PlatformItem.LINUX, ((SnykInstaller) installers.get(0)).getPlatform());
    assertEquals(Long.valueOf(36L), ((SnykInstaller) installers.get(0)).getUpdatePolicyIntervalHours());
    assertEquals("1.947.0", ((SnykInstaller) installers.get(0)).getVersion());
  }
}
