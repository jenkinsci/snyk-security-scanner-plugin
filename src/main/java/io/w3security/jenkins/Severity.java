package io.w3security.jenkins;

import java.util.stream.Stream;

public enum Severity {
  LOW("low"),
  MEDIUM("medium"),
  HIGH("high"),
  CRITICAL("critical");

  private final String severity;

  Severity(String severity) {
    this.severity = severity;
  }

  public String getSeverity() {
    return severity;
  }

  public static Severity getIfPresent(String severity) {
    if (severity == null) {
      return null;
    }

    return Stream.of(Severity.values())
        .filter(entry -> entry.getSeverity().equalsIgnoreCase(severity))
        .findFirst().orElse(null);
  }
}
