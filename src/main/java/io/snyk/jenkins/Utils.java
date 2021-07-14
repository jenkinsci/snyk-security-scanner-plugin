package io.snyk.jenkins;

import java.time.Instant;

public class Utils {
  public static String getURLSafeDateTime() {
    return Instant.now().toString()
      .replaceAll(":", "-")
      .replaceAll("\\.", "-");
  }
}
