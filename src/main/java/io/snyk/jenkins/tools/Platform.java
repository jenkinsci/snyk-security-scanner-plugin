package io.snyk.jenkins.tools;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;

/**
 * Supported platform.
 */
public enum Platform {
  LINUX("snyk-linux", "snyk-to-html-linux"),
  LINUX_ALPINE("snyk-alpine", "snyk-to-html-alpine"),
  MAC_OS("snyk-macos", "snyk-to-html-macos"),
  WINDOWS("snyk-win.exe", "snyk-to-html-win.exe");

  public final String snykWrapperFileName;
  public final String snykToHtmlWrapperFileName;

  Platform(String snykWrapperFileName, String snykToHtmlWrapperFileName) {
    this.snykWrapperFileName = snykWrapperFileName;
    this.snykToHtmlWrapperFileName = snykToHtmlWrapperFileName;
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
