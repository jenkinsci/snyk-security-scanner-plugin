package io.snyk.jenkins.exception;

import hudson.AbortException;

public class SnykErrorException extends AbortException {
  public SnykErrorException(String message) {
    super("Snyk failed to scan your project. " + message);
  }
}
