package io.snyk.tools;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;

import hudson.model.Computer;
import hudson.model.Node;
import io.snyk.Messages;

/**
 * Supported platform.
 */
public enum Platform {
  LINUX("node", "npm", "bin"),
  WINDOWS("node.exe", "npm.cmd", "");

  public final String nodeFileName;
  public final String npmFileName;
  public final String binFolder;

  Platform(String nodeFileName, String npmFileName, String binFolder) {
    this.nodeFileName = nodeFileName;
    this.npmFileName = npmFileName;
    this.binFolder = binFolder;
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
        throw new ToolDetectionException(Messages.Tools_nodeNotAvailable(node.getDisplayName()));
      }
      return detect(computer.getSystemProperties());
    } catch (Exception ex) {
      throw new ToolDetectionException(Messages.Tools_failureOnProperties(), ex);
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
    } else if (arch.contains("windows")) {
      return WINDOWS;
    }
    throw new ToolDetectionException(Messages.Platform_unknown(arch));
  }
}
