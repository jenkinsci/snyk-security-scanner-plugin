package io.snyk.tools;

import java.io.IOException;

class ToolDetectionException extends IOException {
  ToolDetectionException(String message) {
    super(message);
  }

  ToolDetectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
