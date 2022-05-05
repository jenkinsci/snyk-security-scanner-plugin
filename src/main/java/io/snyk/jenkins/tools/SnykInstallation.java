package io.snyk.jenkins.tools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import io.snyk.jenkins.SnykContext;
import io.snyk.jenkins.SnykStepBuilder.SnykStepBuilderDescriptor;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SnykInstallation extends ToolInstallation implements EnvironmentSpecific<SnykInstallation>, NodeSpecific<SnykInstallation> {
  private static final Logger LOG = LoggerFactory.getLogger(SnykInstallation.class);

  @DataBoundConstructor
  public SnykInstallation(@Nonnull String name, @Nullable String home, List<? extends ToolProperty<?>> properties) {
    super(name, home, properties);
  }

  @Override
  public SnykInstallation forEnvironment(EnvVars environment) {
    return new SnykInstallation(getName(), environment.expand(getHome()), getProperties().toList());
  }

  @Override
  public SnykInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
    return new SnykInstallation(getName(), translateFor(node, log), getProperties().toList());
  }

  public String getSnykExecutable(@Nonnull Launcher launcher) throws IOException, InterruptedException {
    return resolveExecutable(launcher, "snyk");
  }

  public String getReportExecutable(@Nonnull Launcher launcher) throws IOException, InterruptedException {
    return resolveExecutable(launcher, "snyk-to-html");
  }

  private String resolveExecutable(Launcher launcher, String executableName) throws IOException, InterruptedException {
    String home = Optional.ofNullable(getHome())
      .orElseThrow(() -> new RuntimeException("Failed to find Snyk Executable. Installation Home is not configured."));

    Platform platform = null;
    SnykStepBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(SnykStepBuilderDescriptor.class);
    SnykInstallation snykInstallation = Stream.of(descriptor.getInstallations())
                                              .filter(installation -> installation.getName().equals(this.getName()))
                                              .findFirst().orElse(null);
    if (snykInstallation != null) {
      PlatformItem installerPlatform = getSnykInstallerPlatformIfDefined(snykInstallation);
      platform = PlatformItem.convert(installerPlatform);
    }

    return Optional.ofNullable(launcher.getChannel())
      .orElseThrow(() -> new RuntimeException("Failed to find Snyk Executable. Build Node does not support channels."))
      .call(new ResolveExecutable(home, executableName, platform));
  }

  @Nonnull
  private PlatformItem getSnykInstallerPlatformIfDefined(SnykInstallation installation) {
    // read saved xml configuration and try to read platform value
    // if nothing will be found or any exceptions occurs return AUTO as default
    DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = installation.getProperties();
    try {
      if (properties.size() == 1 && properties.get(0) instanceof InstallSourceProperty) {
        DescribableList<ToolInstaller, Descriptor<ToolInstaller>> installers = ((InstallSourceProperty) properties.get(0)).installers;
        if (installers.size() == 1 && installers.get(0) instanceof SnykInstaller) {
          return ((SnykInstaller) installers.get(0)).getPlatform();
        }
      }
    } catch(Exception ex) {
      LOG.warn("Could not read properties from Snyk installation", ex);
    }
    LOG.warn("Could not find defined installer architecture, will return 'AUTO'");
    return PlatformItem.AUTO;
  }

  public static SnykInstallation install(SnykContext context, String name) throws IOException, InterruptedException {
    Node node = Optional.ofNullable(context.getWorkspace().toComputer())
      .map(Computer::getNode)
      .orElseThrow(() -> new RuntimeException("Failed to install Snyk. Snyk can only be installed on a Build Node."));

    SnykStepBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(SnykStepBuilderDescriptor.class);
    return Stream.of(descriptor.getInstallations())
      .filter(installation -> installation.getName().equals(name))
      .findFirst()
      .orElseThrow(() -> new RuntimeException("Failed to install Snyk. Installation named '" + name + "' was not found. Please make sure it's configured in Jenkins under Global Tool Configuration."))
      .forNode(node, context.getTaskListener())
      .forEnvironment(context.getEnvVars());
  }

  private static class ResolveExecutable extends MasterToSlaveCallable<String, IOException> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ResolveExecutable.class);

    private final String home;
    private final String executableName;
    private final Platform executablePlatform;

    public ResolveExecutable(String home, String executableName, Platform executablePlatform) {
      this.home = home;
      this.executableName = executableName;
      this.executablePlatform = executablePlatform;
    }

    @Override
    public String call() {
      Platform platform = executablePlatform;
      if (platform == null) {
          LOG.info("Installer architecture is not configured or use AUTO mode");
          platform = Platform.current();
      }
      LOG.info("'{}' platform will be used by resolving Snyk Executable", platform);

      String filename = "snyk".equals(executableName) ? platform.snykWrapperFileName : platform.snykToHtmlWrapperFileName;

      final Path executable = Paths.get(home).resolve(filename).toAbsolutePath();
      if (!executable.toFile().exists()) {
        throw new RuntimeException("Failed to find Snyk Executable. Executable does not exist. (" + executable.toAbsolutePath() + ")");
      }
      return executable.toString();
    }
  }

  @Extension
  @Symbol("snyk")
  public static class SnykInstallationDescriptor extends ToolDescriptor<SnykInstallation> {

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Snyk";
    }

    @Override
    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new SnykInstaller(null, null, null, null));
    }

    @Override
    public SnykInstallation[] getInstallations() {
      return Jenkins.get().getDescriptorByType(SnykStepBuilderDescriptor.class).getInstallations();
    }

    @Override
    public void setInstallations(SnykInstallation... installations) {
      Jenkins.get().getDescriptorByType(SnykStepBuilderDescriptor.class).setInstallations(installations);
    }
  }
}
