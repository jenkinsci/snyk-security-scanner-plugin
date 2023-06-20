package io.w3security.jenkins.jcasc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.w3security.jenkins.tools.PlatformItem;
import io.w3security.jenkins.tools.W3SecurityInstallation;
import io.w3security.jenkins.tools.W3SecurityInstaller;
import org.junit.Rule;
import org.junit.Test;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import static org.junit.Assert.assertEquals;

public class ConfigurationAsCodeTest {

  @Rule
  public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

  @Test
  @ConfiguredWithCode("configuration-as-code.yml")
  public void should_support_configuration_as_code() throws Exception {
    W3SecurityInstallation.W3SecurityInstallationDescriptor descriptor = ExtensionList
        .lookupSingleton(W3SecurityInstallation.W3SecurityInstallationDescriptor.class);
    assertEquals(descriptor.getInstallations().length, 1);

    W3SecurityInstallation installation = descriptor.getInstallations()[0];
    assertEquals("w3security", installation.getName());
    DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = installation.getProperties();
    DescribableList<ToolInstaller, Descriptor<ToolInstaller>> installers = ((InstallSourceProperty) properties
        .get(0)).installers;
    assertEquals(PlatformItem.LINUX, ((W3SecurityInstaller) installers.get(0)).getPlatform());
    assertEquals(Long.valueOf(36l), ((W3SecurityInstaller) installers.get(0)).getUpdatePolicyIntervalHours());
    assertEquals("1.947.0", ((W3SecurityInstaller) installers.get(0)).getVersion());
  }

}
