package io.w3security.jenkins.config;

public interface W3SecurityConfig {
  String getSeverity();

  String getTargetFile();

  String getOrganisation();

  String getProjectName();

  String getAdditionalArguments();

  String getW3SecurityInstallation();

  String getW3SecurityTokenId();

  boolean isMonitorProjectOnBuild();

  boolean isFailOnIssues();

  boolean isFailOnError();

}
