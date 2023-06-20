package io.w3security.jenkins.tools;

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

    switch (platformItem) {
      case LINUX_ALPINE:
        return Platform.LINUX_ALPINE;

      case WINDOWS:
        return Platform.WINDOWS;

      case LINUX:
      default:
        return Platform.LINUX;
    }
  }
}
