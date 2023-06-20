package io.w3security.jenkins.exception;

import hudson.AbortException;

public class W3SecurityIssueException extends AbortException {
  public W3SecurityIssueException() {
    super("W3Security has detected security vulnerabilities in your project.");
  }
}
