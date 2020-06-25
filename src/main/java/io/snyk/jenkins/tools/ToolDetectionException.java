package io.snyk.jenkins.tools;

import java.io.IOException;

class ToolDetectionException extends IOException {

  ToolDetectionException(Throwable cause) {
    super(cause);
  }

  ToolDetectionException(String message) {
    super(message);
  }

  ToolDetectionException(String message, Throwable innerException) {
    super(message, innerException);
  }
}
