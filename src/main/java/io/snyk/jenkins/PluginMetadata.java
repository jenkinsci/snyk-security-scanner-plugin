package io.snyk.jenkins;

import hudson.Plugin;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

import java.util.Optional;

public class PluginMetadata {

  private static final String INTEGRATION_NAME = "JENKINS";
  private static String INTEGRATION_VERSION;

  public static String getIntegrationName() {
    return INTEGRATION_NAME;
  }

  public static String getIntegrationVersion() {
    if (INTEGRATION_VERSION == null) {
      INTEGRATION_VERSION = Optional.ofNullable(Jenkins.get().getPlugin("snyk-security-scanner"))
        .map(Plugin::getWrapper)
        .map(PluginWrapper::getVersion)
        .orElse("unknown");
    }
    return INTEGRATION_VERSION;
  }
}
