package io.snyk.jenkins.config;

public interface SnykConfig {
  String getSeverity();

  String getTargetFile();

  String getOrganisation();

  String getProjectName();

  String getAdditionalArguments();
}
