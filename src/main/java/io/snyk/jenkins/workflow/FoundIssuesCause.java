package io.snyk.jenkins.workflow;

import jenkins.model.CauseOfInterruption;

public final class FoundIssuesCause extends CauseOfInterruption {
  @Override
  public String getShortDescription() {
    return "Snyk has detected security vulnerabilities in your project.";
  }
}
