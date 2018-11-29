package io.snyk.jenkins.tools;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import io.snyk.jenkins.tools.SnykBuildWrapper.SnykBuildWrapperDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class SnykInstallation extends ToolInstallation implements EnvironmentSpecific<SnykInstallation>, NodeSpecific<SnykInstallation> {

  private transient Platform platform;

  @DataBoundConstructor
  public SnykInstallation(@Nonnull String name, @Nonnull String home, List<? extends ToolProperty<?>> properties) {
    this(name, home, properties, null);
  }

  private SnykInstallation(@Nonnull String name, @Nonnull String home, List<? extends ToolProperty<?>> properties, Platform platform) {
    super(name, home, properties);
    this.platform = platform;
  }

  @Override
  public void buildEnvVars(EnvVars env) {
    String root = getHome();
    if (root != null) {
      env.put("PATH+SNYK_HOME", new File(root, "node_modules/.bin").toString());
    }
  }

  @Override
  public SnykInstallation forEnvironment(EnvVars environment) {
    return new SnykInstallation(getName(), environment.expand(getHome()), getProperties().toList());
  }

  @Override
  public SnykInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
    return new SnykInstallation(getName(), translateFor(node, log), getProperties().toList());
  }

  @Extension
  @Symbol("snyk")
  public static class SnykInstallationDescriptor extends ToolDescriptor<SnykInstallation> {

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Snyk";
    }

    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new SnykInstaller(null, null, null));
    }

    @Override
    public SnykInstallation[] getInstallations() {
      Jenkins instance = Jenkins.getInstanceOrNull();
      if (instance == null) {
        throw new IllegalStateException("Jenkins has not been started, or was already shut down");
      }
      return instance.getDescriptorByType(SnykBuildWrapperDescriptor.class).getInstallations();
    }

    @Override
    public void setInstallations(SnykInstallation... installations) {
      Jenkins instance = Jenkins.getInstanceOrNull();
      if (instance == null) {
        throw new IllegalStateException("Jenkins has not been started, or was already shut down");
      }
      instance.getDescriptorByType(SnykBuildWrapperDescriptor.class).setInstallations(installations);
    }
  }
}
