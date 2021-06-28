package io.snyk.jenkins.exception;

import hudson.AbortException;

public class SnykIssueException extends AbortException {
  public SnykIssueException() {
    super("Snyk has detected security vulnerabilities in your project.");
  }
}
