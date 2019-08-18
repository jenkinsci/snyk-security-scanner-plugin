package io.snyk.jenkins.workflow;

import jenkins.model.CauseOfInterruption;

public final class FoundIssuesCause extends CauseOfInterruption {
  @Override
  public String getShortDescription() {
    return "Snyk detected vulnerability issues.";
  }
}
