package io.snyk.jenkins.tools;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;

import hudson.model.Computer;
import hudson.model.Node;

/**
 * Supported platform.
 */
public enum Platform {
  LINUX("node", "npm", "bin", "snyk-linux", "snyk-to-html-linux"),
  MAC_OS("node", "npm", "bin", "snyk-macos", "snyk-to-html-macos"),
  WINDOWS("node.exe", "npm.cmd", "", "snyk-win.exe", "snyk-to-html-win.exe");

  public final String nodeFileName;
  public final String npmFileName;
  public final String binFolder;
  public final String snykWrapperFileName;
  public final String snykToHtmlWrapperFileName;

  Platform(String nodeFileName, String npmFileName, String binFolder, String snykWrapperFileName, String snykToHtmlWrapperFileName) {
    this.nodeFileName = nodeFileName;
    this.npmFileName = npmFileName;
    this.binFolder = binFolder;
    this.snykWrapperFileName = snykWrapperFileName;
    this.snykToHtmlWrapperFileName = snykToHtmlWrapperFileName;
  }

  /**
   * Determines the platform of the given node.
   *
   * @param node the computer node
   * @return a platform value that represent the given node
   * @throws ToolDetectionException when the current platform node is not supported or not available
   */
  @Nonnull
  public static Platform of(Node node) throws ToolDetectionException {
    try {
      Computer computer = node.toComputer();
      if (computer == null) {
        throw new ToolDetectionException("Node '" + node.getDisplayName() + "' has no executors");
      }
      return detect(computer.getSystemProperties());
    } catch (Exception ex) {
      throw new ToolDetectionException(ex);
    }
  }

  @Nonnull
  public static Platform current() throws ToolDetectionException {
    return detect(System.getProperties());
  }

  @Nonnull
  private static Platform detect(@Nonnull Map<Object, Object> systemProperties) throws ToolDetectionException {
    String arch = ((String) systemProperties.get("os.name")).toLowerCase(Locale.ENGLISH);
    if (arch.contains("linux")) {
      return LINUX;
    } else if (arch.contains("mac os x") || arch.contains("darwin") || arch.contains("osx")) {
      return MAC_OS;
    } else if (arch.contains("windows")) {
      return WINDOWS;
    }
    throw new ToolDetectionException(arch + " is not supported CPU type");
  }
}
