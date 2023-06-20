package io.w3security.jenkins.tools;

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
import io.w3security.jenkins.W3SecurityContext;
import io.w3security.jenkins.W3SecurityStepBuilder.W3SecurityStepBuilderDescriptor;
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

public class W3SecurityInstallation extends ToolInstallation
    implements EnvironmentSpecific<W3SecurityInstallation>, NodeSpecific<W3SecurityInstallation> {
  private static final Logger LOG = LoggerFactory.getLogger(W3SecurityInstallation.class);

  @DataBoundConstructor
  public W3SecurityInstallation(@Nonnull String name, @Nullable String home, List<? extends ToolProperty<?>> properties) {
    super(name, home, properties);
  }

  @Override
  public W3SecurityInstallation forEnvironment(EnvVars environment) {
    return new W3SecurityInstallation(getName(), environment.expand(getHome()), getProperties().toList());
  }

  @Override
  public W3SecurityInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
    return new W3SecurityInstallation(getName(), translateFor(node, log), getProperties().toList());
  }

  public String getW3SecurityExecutable(@Nonnull Launcher launcher) throws IOException, InterruptedException {
    return resolveExecutable(launcher, "w3security");
  }

  public String getReportExecutable(@Nonnull Launcher launcher) throws IOException, InterruptedException {
    return resolveExecutable(launcher, "w3security-to-html");
  }

  private String resolveExecutable(Launcher launcher, String executableName) throws IOException, InterruptedException {
    String home = Optional.ofNullable(getHome())
        .orElseThrow(
            () -> new RuntimeException("Failed to find W3Security Executable. Installation Home is not configured."));

    Platform platform = null;
    W3SecurityStepBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(W3SecurityStepBuilderDescriptor.class);
    W3SecurityInstallation w3securityInstallation = Stream.of(descriptor.getInstallations())
        .filter(installation -> installation.getName().equals(this.getName()))
        .findFirst().orElse(null);
    if (w3securityInstallation != null) {
      PlatformItem installerPlatform = getW3SecurityInstallerPlatformIfDefined(w3securityInstallation);
      platform = PlatformItem.convert(installerPlatform);
    }

    return Optional.ofNullable(launcher.getChannel())
        .orElseThrow(
            () -> new RuntimeException("Failed to find W3Security Executable. Build Node does not support channels."))
        .call(new ResolveExecutable(home, executableName, platform));
  }

  @Nonnull
  private PlatformItem getW3SecurityInstallerPlatformIfDefined(W3SecurityInstallation installation) {
    // read saved xml configuration and try to read platform value
    // if nothing will be found or any exceptions occurs return AUTO as default
    DescribableList<ToolProperty<?>, ToolPropertyDescriptor> properties = installation.getProperties();
    try {
      if (properties.size() == 1 && properties.get(0) instanceof InstallSourceProperty) {
        DescribableList<ToolInstaller, Descriptor<ToolInstaller>> installers = ((InstallSourceProperty) properties
            .get(0)).installers;
        if (installers.size() == 1 && installers.get(0) instanceof W3SecurityInstaller) {
          return ((W3SecurityInstaller) installers.get(0)).getPlatform();
        }
      }
    } catch (Exception ex) {
      LOG.warn("Could not read properties from W3Security installation", ex);
    }
    LOG.warn("Could not find defined installer architecture, will return 'AUTO'");
    return PlatformItem.AUTO;
  }

  public static W3SecurityInstallation install(W3SecurityContext context, String name) throws IOException, InterruptedException {
    Node node = Optional.ofNullable(context.getWorkspace().toComputer())
        .map(Computer::getNode)
        .orElseThrow(() -> new RuntimeException("Failed to install W3Security. W3Security can only be installed on a Build Node."));

    W3SecurityStepBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(W3SecurityStepBuilderDescriptor.class);
    return Stream.of(descriptor.getInstallations())
        .filter(installation -> installation.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Failed to install W3Security. Installation named '" + name
            + "' was not found. Please make sure it's configured in Jenkins under Global Tool Configuration."))
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
      LOG.info("'{}' platform will be used by resolving W3Security Executable", platform);

      String filename = "w3security".equals(executableName) ? platform.w3securityWrapperFileName
          : platform.w3securityToHtmlWrapperFileName;

      final Path executable = Paths.get(home).resolve(filename).toAbsolutePath();
      if (!executable.toFile().exists()) {
        throw new RuntimeException(
            "Failed to find W3Security Executable. Executable does not exist. (" + executable.toAbsolutePath() + ")");
      }
      return executable.toString();
    }
  }

  @Extension
  @Symbol("w3security")
  public static class W3SecurityInstallationDescriptor extends ToolDescriptor<W3SecurityInstallation> {

    @Nonnull
    @Override
    public String getDisplayName() {
      return "W3Security";
    }

    @Override
    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new W3SecurityInstaller(null, null, null, null));
    }

    @Override
    public W3SecurityInstallation[] getInstallations() {
      return Jenkins.get().getDescriptorByType(W3SecurityStepBuilderDescriptor.class).getInstallations();
    }

    @Override
    public void setInstallations(W3SecurityInstallation... installations) {
      Jenkins.get().getDescriptorByType(W3SecurityStepBuilderDescriptor.class).setInstallations(installations);
    }
  }
}
