package io.snyk.jenkins.config;

public interface SnykConfig {
  String getSeverity();

  String getTargetFile();

  String getOrganisation();

  String getProjectName();

  String getAdditionalArguments();

  String getSnykInstallation();

  String getSnykTokenId();

  boolean isMonitorProjectOnBuild();

  boolean isFailOnIssues();

  boolean isFailOnError();

}
