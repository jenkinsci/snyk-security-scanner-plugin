package io.snyk.jenkins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.join;
import static org.apache.commons.lang.StringUtils.chomp;

class CustomBuildToolPathCallable implements FilePath.FileCallable<String> {

  private static final Logger LOG = LoggerFactory.getLogger(CustomBuildToolPathCallable.class.getName());
  private static final long serialVersionUID = 1L;

  @Override
  public String invoke(File snykToolDirectory, VirtualChannel channel) {
    String oldPath = System.getenv("PATH");
    String home = snykToolDirectory.getAbsolutePath();
    String toolsDirectory = home.substring(0, home.indexOf("tools") - 1) + File.separator + "tools";

    try (Stream<Path> toolsSubDirectories = Files.walk(Paths.get(toolsDirectory))) {
      List<String> toolsPaths = new ArrayList<>();
      toolsSubDirectories.filter(Files::isDirectory)
                         .filter(path -> !path.toString().contains("SnykInstallation"))
                         .filter(path -> !path.toString().contains("jansi-native"))
                         .forEach(entry -> toolsPaths.add(chomp(entry.toAbsolutePath().toString())));

      String customBuildToolPath = join(File.pathSeparator, toolsPaths);
      return oldPath + File.pathSeparator + customBuildToolPath;
    } catch (IOException ex) {
      LOG.error("Could not iterate sub-directories in tools directory", ex);
      return oldPath;
    }
  }

  @Override
  public void checkRoles(RoleChecker roleChecker) {
    //squid:S1186
  }
}
