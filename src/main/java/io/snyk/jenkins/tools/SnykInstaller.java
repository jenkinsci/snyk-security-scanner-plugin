package io.snyk.jenkins.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.tools.internal.DownloadService;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import static hudson.Util.fixEmptyAndTrim;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;

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
  public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
    FilePath expected = preferredLocation(tool, node);

    if (isUpToDate(expected)) {
      log.getLogger().println("Snyk installation is UP-TO-DATE");
      return expected;
    }

    if (isNpmAvailable(node, log)) {
      LOG.log(INFO, format("NodeJS is available on this node: '%s'. Snyk will be installed as NPM package.", node.getDisplayName()));
      return installSnykAsNpmPackage(expected, node, log);
    } else {
      LOG.log(INFO, format("NodeJS is not available on this node: '%s'. Snyk will be installed as single binary.", node.getDisplayName()));
      return installSnykAsSingleBinary(expected, node, log);
    }
  }

  private boolean isUpToDate(FilePath expectedLocation) throws IOException, InterruptedException {
    FilePath marker = expectedLocation.child(".timestamp");
    if (!marker.exists()) {
      return false;
    }

    String content = marker.readToString();
    long timestampFromFile = Long.parseLong(content);
    long timestampNow = Instant.now().toEpochMilli();

    long timestampDifference = timestampNow - timestampFromFile;
    if (timestampDifference <= 0) {
      return true;
    }
    long updateInterval = TimeUnit.HOURS.toMillis(updatePolicyIntervalHours);
    return timestampDifference < updateInterval;
  }

  private boolean isNpmAvailable(Node node, TaskListener log) {
    Launcher launcher = node.createLauncher(log);
    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps.quiet(true).cmds("npm", "--version");

    try {
      int exitCode = launcher.launch(ps).join();
      return exitCode == 0;
    } catch (Exception ex) {
      LOG.log(INFO, format("NPM is not available on the node: '%s'", node.getDisplayName()));
      LOG.log(FINEST, "'npm --version' command failed", ex);
      return false;
    }
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "silly rule")
  private FilePath installSnykAsNpmPackage(FilePath expected, Node node, TaskListener log) {
    log.getLogger().println("Installing Snyk Security tool via NPM (version '" + fixEmptyAndTrim(version) + "')");
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add("npm", "install", "--prefix", expected.getRemote(), "snyk@" + fixEmptyAndTrim(version), "snyk-to-html");
    Launcher launcher = node.createLauncher(log);
    Launcher.ProcStarter ps = launcher.new ProcStarter();
    ps.quiet(true).cmds(args);

    try {
      int exitCode = launcher.launch(ps).join();
      if (exitCode != 0) {
        log.getLogger().println("Snyk installation was not successful. Exit code: " + exitCode);
        return expected;
      }
      expected.child(".timestamp").write(valueOf(Instant.now().toEpochMilli()), "UTF-8");
    } catch (Exception ex) {
      log.getLogger().println("Could not install snyk via NPM");
    }
    return expected;
  }

  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private FilePath installSnykAsSingleBinary(FilePath expected, Node node, TaskListener log) throws IOException, InterruptedException {
    log.getLogger().println("Installing Snyk Security tool as single binary (version '" + fixEmptyAndTrim(version) + "')");

    if (node.getChannel() == null) {
      throw new IOException(format("Node '%s' is offline", node.getDisplayName()));
    }

    Platform platform = node.getChannel().call(new GetPlatform(node.getDisplayName()));

    try {
      URL snykDownloadUrl = DownloadService.getDownloadUrlForSnyk(version, platform);
      URL snykToHtmlDownloadUrl = DownloadService.getDownloadUrlForSnykToHtml(platform);
      expected.mkdirs();
      node.getChannel().call(new Downloader(snykDownloadUrl, expected.child(platform.snykWrapperFileName)));
      node.getChannel().call(new Downloader(snykToHtmlDownloadUrl, expected.child(platform.snykToHtmlWrapperFileName)));
      expected.child(".timestamp").write(valueOf(Instant.now().toEpochMilli()), "UTF-8");
      expected.child(".installedFrom").write(snykDownloadUrl.toString(), "UTF-8");
    } catch (Exception ex) {
      throw new IOException("Could not install snyk binary", ex);
    }

    return expected;
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
      //TODO: fix naming
      return "Install as global NPM package";
    }

    @Override
    public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
      return toolType == SnykInstallation.class;
    }
  }

  private static class GetPlatform extends MasterToSlaveCallable<Platform, IOException> {
    private static final long serialVersionUID = 1L;

    private final String nodeDisplayName;

    GetPlatform(String nodeDisplayName) {
      this.nodeDisplayName = nodeDisplayName;
    }

    @Override
    public Platform call() throws IOException {
      try {
        return Platform.current();
      } catch (ToolDetectionException e) {
        throw new IOException(format("Could not determine platform on node %s", nodeDisplayName));
      }
    }
  }

  private static class Downloader extends MasterToSlaveCallable<Void, IOException> {
    private static final long serialVersionUID = 1L;

    private final URL downloadUrl;
    private final FilePath output;

    Downloader(URL downloadUrl, FilePath output) {
      this.downloadUrl = downloadUrl;
      this.output = output;
    }

    @Override
    public Void call() throws IOException {
      final File downloadedFile = new File(output.getRemote());
      FileUtils.copyURLToFile(downloadUrl, downloadedFile, 10000, 10000);
      // set execute permission
      if (!Functions.isWindows() && downloadedFile.isFile()) {
        boolean result = downloadedFile.setExecutable(true, false);
        if (!result) {
          throw new IOException(format("Could not set executable flag for the file: %s", downloadedFile.getAbsolutePath()));
        }
      }
      return null;
    }
  }
}
