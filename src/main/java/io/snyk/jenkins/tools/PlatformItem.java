package io.snyk.jenkins.tools;

import javax.annotation.Nullable;

/**
 * Used in UI config.jelly for overriding platform detection if needed.
 */
public enum PlatformItem {
  AUTO,
  LINUX,
  LINUX_ALPINE,
  MAC_OS,
  WINDOWS;

  @Nullable
  public static Platform convert(@Nullable PlatformItem platformItem) {
    if (platformItem == null || platformItem == AUTO) {
      return null;
    }

      return switch (platformItem) {
          case LINUX_ALPINE -> Platform.LINUX_ALPINE;
          case WINDOWS -> Platform.WINDOWS;
          default -> Platform.LINUX;
      };
  }
}
