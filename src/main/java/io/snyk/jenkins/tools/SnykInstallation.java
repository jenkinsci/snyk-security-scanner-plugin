package io.snyk.jenkins.tools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import io.snyk.jenkins.SnykContext;
import io.snyk.jenkins.SnykStepBuilder.SnykStepBuilderDescriptor;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

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

  private String resolveExecutable(Launcher launcher, String name) throws IOException, InterruptedException {
    String home = Optional.ofNullable(getHome())
      .orElseThrow(() -> new RuntimeException("Failed to find Snyk Executable. Installation Home is not configured."));

    return Optional.ofNullable(launcher.getChannel())
      .orElseThrow(() -> new RuntimeException("Failed to find Snyk Executable. Build Node does not support channels."))
      .call(new ResolveExecutable(home, name));
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

    private final String home;
    private final String name;

    public ResolveExecutable(String home, String name) {
      this.home = home;
      this.name = name;
    }

    @Override
    public String call() {
      Platform platform = Platform.current();
      String filename = "snyk".equals(name)
        ? platform.snykWrapperFileName
        : platform.snykToHtmlWrapperFileName;

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
      return Collections.singletonList(new SnykInstaller(null, null, null));
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
