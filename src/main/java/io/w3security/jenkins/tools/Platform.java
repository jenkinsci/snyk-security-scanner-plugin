package io.w3security.jenkins.tools;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;

/**
 * Supported platform.
 */
public enum Platform {
  LINUX("w3security-linux", "w3security-to-html-linux"),
  LINUX_ALPINE("w3security-alpine", "w3security-to-html-alpine"),
  MAC_OS("w3security-macos", "w3security-to-html-macos"),
  WINDOWS("w3security-win.exe", "w3security-to-html-win.exe");

  public final String w3securityWrapperFileName;
  public final String w3securityToHtmlWrapperFileName;

  Platform(String w3securityWrapperFileName, String w3securityToHtmlWrapperFileName) {
    this.w3securityWrapperFileName = w3securityWrapperFileName;
    this.w3securityToHtmlWrapperFileName = w3securityToHtmlWrapperFileName;
  }

  @Nonnull
  public static Platform current() {
    return detect(System.getProperties());
  }

  @Nonnull
  private static Platform detect(@Nonnull Map<Object, Object> systemProperties) {
    String osName = ((String) systemProperties.get("os.name")).toLowerCase(Locale.ENGLISH);
    String osArch = ((String) systemProperties.get("os.arch")).toLowerCase(Locale.ENGLISH);
    if (osName.contains("linux")) {
      // detect whether we run on linux alpine
      if (osArch.contains("amd64") || osArch.contains("86_64")) {
        return LINUX;
      } else {
        return LINUX_ALPINE;
      }
    } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
      return MAC_OS;
    } else if (osName.contains("windows")) {
      return WINDOWS;
    }
    throw new RuntimeException("Unsupported platform. (OS: " + osName + ", Arch: " + osArch + ")");
  }
}
