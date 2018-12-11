package io.snyk.jenkins.tools;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import static hudson.Util.fixEmptyAndTrim;
import static java.lang.String.valueOf;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

public class SnykInstaller extends ToolInstaller {

  private static final Logger LOG = Logger.getLogger(SnykInstaller.class.getName());

  private final String version;
  private final Long updatePolicyIntervalHours;

  @DataBoundConstructor
  public SnykInstaller(String label, String version, Long updatePolicyIntervalHours) {
    super(label);
    this.version = version;
    this.updatePolicyIntervalHours = updatePolicyIntervalHours;
  }

  @Override
  public FilePath performInstallation(ToolInstallation toolInstallation, Node node, TaskListener log) throws IOException, InterruptedException {
    FilePath expected = preferredLocation(tool, node);

    if (!isNpmAvailable(node, log)) {
      //TODO: message for end user
      log.getLogger().println("NodeJS is not available on this node: " + node.getDisplayName());
      return expected;
    }

    if (isUpToDate(expected)) {
      LOG.log(INFO, "No Snyk installation -> up-to-date");
      return expected;
    }

    // install snyk
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add("npm", "install", "--prefix", expected.getRemote(), "snyk@" + fixEmptyAndTrim(version));
    Launcher launcher = node.createLauncher(log);
    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps.cmds(args).stdout(log);

    try {
      int exitCode = launcher.launch(ps).join();
      if (exitCode != 0) {
        log.getLogger().print("Snyk installation was not successful. Exit code: " + exitCode);
      }
      expected.child(".timestamp").write(valueOf(Instant.now().toEpochMilli()), "UTF-8");
    } catch (Exception ex) {
      log.getLogger().println("Could not install snyk via NPM");
    }
    return expected;
  }

  private boolean isNpmAvailable(Node node, TaskListener log) {
    Launcher launcher = node.createLauncher(log);
    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps.quiet(true).cmds("npm", "--version");

    try {
      int exitCode = launcher.launch(ps).join();
      return exitCode == 0;
    } catch (Exception ex) {
      LOG.log(SEVERE, "Could not check if NPM is available" + ex);
      return false;
    }
  }

  private boolean isUpToDate(FilePath expectedLocation) throws IOException, InterruptedException {
    FilePath marker = expectedLocation.child(".timestamp");
    if (!marker.exists()) {
      return false;
    }

    String content = marker.readToString();
    //TODO: handle parsing exceptions
    long timestampFromFile = Long.valueOf(content);
    long timestampNow = Instant.now().toEpochMilli();

    long timestampDifference = timestampNow - timestampFromFile;
    if (timestampDifference <= 0) {
      return true;
    }
    long updateInterval = TimeUnit.HOURS.toMillis(updatePolicyIntervalHours);
    return timestampDifference < updateInterval;
  }

  @SuppressWarnings("unused")
  public String getVersion() {
    return version;
  }

  @SuppressWarnings("unused")
  public Long getUpdatePolicyIntervalHours() {
    return updatePolicyIntervalHours;
  }

  @Extension
  public static final class SnykInstallerDescriptor extends ToolInstallerDescriptor<SnykInstaller> {

    @Override
    public String getDisplayName() {
      return "Install as global NPM package";
    }

    @Override
    public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
      return toolType == SnykInstallation.class;
    }
  }
}
