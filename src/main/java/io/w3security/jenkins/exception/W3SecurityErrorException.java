package io.w3security.jenkins.exception;

import hudson.AbortException;

public class W3SecurityErrorException extends AbortException {
  public W3SecurityErrorException(String message) {
    super("W3Security failed to scan your project. " + message);
  }
}
