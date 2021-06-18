package io.snyk.jenkins.tools;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import io.snyk.jenkins.SnykStepBuilder.SnykStepBuilderDescriptor;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class SnykInstallation extends ToolInstallation implements EnvironmentSpecific<SnykInstallation>, NodeSpecific<SnykInstallation> {

  @DataBoundConstructor
  public SnykInstallation(@Nonnull String name, @Nonnull String home, List<? extends ToolProperty<?>> properties) {
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
    VirtualChannel channel = launcher.getChannel();
    return channel == null ? null : channel.call(new MasterToSlaveCallable<String, IOException>() {
      @Override
      public String call() throws IOException {
        return resolveExecutable("snyk", Platform.current());
      }
    });
  }

  public String getReportExecutable(@Nonnull Launcher launcher) throws IOException, InterruptedException {
    VirtualChannel channel = launcher.getChannel();
    return channel == null ? null : channel.call(new MasterToSlaveCallable<String, IOException>() {
      @Override
      public String call() throws IOException {
        return resolveExecutable("snyk-to-html", Platform.current());
      }
    });
  }

  private String resolveExecutable(String file, Platform platform) throws IOException {
    String root = getHome();
    if (root == null) {
      return null;
    }
    String wrapperFileName = "snyk".equals(file) ? platform.snykWrapperFileName : platform.snykToHtmlWrapperFileName;
    final Path executable = Paths.get(root).resolve(wrapperFileName);
    if (!executable.toFile().exists()) {
      throw new IOException(format("Could not find executable <%s>", wrapperFileName));
    }
    return executable.toAbsolutePath().toString();
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
