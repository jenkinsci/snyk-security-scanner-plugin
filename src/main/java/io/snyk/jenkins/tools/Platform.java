package io.snyk.jenkins.tools;

import javax.annotation.Nonnull;
import java.nio.file.Paths;
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
  public static Platform current() throws ToolDetectionException {
    return detect(System.getProperties());
  }

  @Nonnull
  private static Platform detect(@Nonnull Map<Object, Object> systemProperties) throws ToolDetectionException {
    String arch = ((String) systemProperties.get("os.name")).toLowerCase(Locale.ENGLISH);
    if (arch.contains("linux")) {
      return Paths.get("/etc/alpine-release").toFile().exists() ? LINUX_ALPINE : LINUX;
    } else if (arch.contains("mac os x") || arch.contains("darwin") || arch.contains("osx")) {
      return MAC_OS;
    } else if (arch.contains("windows")) {
      return WINDOWS;
    }
    throw new ToolDetectionException(arch + " is not supported CPU type");
  }
}
