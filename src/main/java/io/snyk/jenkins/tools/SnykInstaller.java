package io.snyk.jenkins.tools;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.tools.internal.DownloadService;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static hudson.Util.fixEmptyAndTrim;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykInstaller extends ToolInstaller {

  private static final Logger LOG = LoggerFactory.getLogger(SnykInstaller.class.getName());
  private static final String INSTALLED_FROM = ".installedFrom";
  private static final String TIMESTAMP_FILE = ".timestamp";

  private final String version;
  private final Long updatePolicyIntervalHours;
  private final PlatformItem platform;

  @DataBoundConstructor
  public SnykInstaller(String label, String version, Long updatePolicyIntervalHours, PlatformItem platform) {
    super(label);
    this.version = version;
    this.updatePolicyIntervalHours = updatePolicyIntervalHours;
    this.platform = platform;
  }

  @Override
  public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener listener) throws IOException, InterruptedException {
    FilePath expected = preferredLocation(tool, node);
    PrintStream logger = listener.getLogger();

    if (isUpToDate(expected)) {
      return expected;
    }

    logger.println("Installing Snyk (" + fixEmptyAndTrim(version) + ")...");
    return downloadSnykBinaries(expected, node);
  }

  private boolean isUpToDate(FilePath expectedLocation) throws IOException, InterruptedException {
    FilePath marker = expectedLocation.child(TIMESTAMP_FILE);
    if (!marker.exists()) {
      return false;
    }

    String content = StringUtils.chomp(marker.readToString());
    long timestampFromFile;
    try {
      timestampFromFile = Long.parseLong(content);
    } catch (NumberFormatException ex) {
      // corrupt of modified .timestamp file => force new installation
      LOG.error(".timestamp file is corrupt and cannot be read and will be reset to 0.");
      timestampFromFile = 0;
    }
    long timestampNow = Instant.now().toEpochMilli();

    long timestampDifference = timestampNow - timestampFromFile;
    if (timestampDifference <= 0) {
      return true;
    }
    long updateInterval = TimeUnit.HOURS.toMillis(updatePolicyIntervalHours);
    return timestampDifference < updateInterval;
  }

  private FilePath downloadSnykBinaries(FilePath expected, Node node) {
    for (String snykUrlTemplate : DownloadService.SNYK_CLI_DOWNLOAD_URLS) {
      try {
        LOG.info("Installing Snyk '{}' on Build Node '{}'", version, node.getDisplayName());

        final VirtualChannel nodeChannel = node.getChannel();
        if (nodeChannel == null) {
          throw new IOException(format("Build Node '%s' is offline.", node.getDisplayName()));
        }

        Platform platform = PlatformItem.convert(this.platform);
        if (platform == null) {
          LOG.info("Installer architecture is not configured or use AUTO mode");
          platform = nodeChannel.call(new GetPlatform());
        }
        LOG.info("Configured installer architecture is {}", platform);

        URL snykDownloadUrl = DownloadService.constructDownloadUrlForSnyk(snykUrlTemplate, "cli", version, platform);
        URL snykToHtmlDownloadUrl = DownloadService.constructDownloadUrlForSnyk(snykUrlTemplate, "snyk-to-html", "latest", platform);

        LOG.info("Downloading CLI from {}", snykDownloadUrl);
        LOG.info("Downloading snyk-to-html from {}", snykToHtmlDownloadUrl);

        expected.mkdirs();
        nodeChannel.call(new Downloader(snykDownloadUrl, expected.child(platform.snykWrapperFileName)));
        nodeChannel.call(new Downloader(snykToHtmlDownloadUrl, expected.child(platform.snykToHtmlWrapperFileName)));
        expected.child(INSTALLED_FROM).write(snykDownloadUrl.toString(), UTF_8.name());
        expected.child(TIMESTAMP_FILE).write(valueOf(Instant.now().toEpochMilli()), UTF_8.name());

        return expected;
      } catch (RuntimeException | IOException | InterruptedException ex) {
        LOG.error("Failed to install Snyk.", ex);
      }
    }
    throw new RuntimeException("Failed to install Snyk.");
  }

  @SuppressWarnings("unused")
  public String getVersion() {
    return version;
  }

  @SuppressWarnings("unused")
  public Long getUpdatePolicyIntervalHours() {
    return updatePolicyIntervalHours;
  }

  @SuppressWarnings("unused")
  public PlatformItem getPlatform() {
    return platform;
  }

  @Extension
  public static final class SnykInstallerDescriptor extends ToolInstallerDescriptor<SnykInstaller> {

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Install from snyk.io";
    }

    @Override
    public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
      return toolType == SnykInstallation.class;
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillPlatformItems() {
      ListBoxModel platformItems = new ListBoxModel();

      platformItems.add("Auto-detection", PlatformItem.AUTO.name());
      platformItems.add("Linux (amd64)", PlatformItem.LINUX.name());
      platformItems.add("Linux Alpine (amd64)", PlatformItem.LINUX_ALPINE.name());
      platformItems.add("Mac OS (amd64)", PlatformItem.MAC_OS.name());
      platformItems.add("Windows (amd64)", PlatformItem.WINDOWS.name());

      return platformItems;
    }
  }

  private static class GetPlatform extends MasterToSlaveCallable<Platform, IOException> {
    private static final long serialVersionUID = 1L;

    @Override
    public Platform call() {
      return Platform.current();
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
          throw new RuntimeException(format("Failed to set file as executable. (%s)", downloadedFile.getAbsolutePath()));
        }
      }
      return null;
    }
  }
}
