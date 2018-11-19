package io.snyk.tools;

import javax.annotation.Nonnull;
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
import org.kohsuke.stapler.DataBoundConstructor;

public class SnykInstallation extends ToolInstallation implements EnvironmentSpecific<SnykInstallation>, NodeSpecific<SnykInstallation> {

  @DataBoundConstructor
  public SnykInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
    super(name, home, properties);
  }

  @Override
  public SnykInstallation forEnvironment(EnvVars envVars) {
    return null;
  }

  @Override
  public SnykInstallation forNode(@Nonnull Node node, TaskListener taskListener) {
    return null;
  }

  @Extension
  //TODO: symbol
  public static class DescriptorImpl extends ToolDescriptor<SnykInstallation> {

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Snyk";
    }

    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new SnykInstaller(null));
    }

    @Override
    public SnykInstallation[] getInstallations() {
      //TODO: handle installation on master/node
      return super.getInstallations();
    }

    @Override
    public void setInstallations(SnykInstallation... installations) {
      //TODO: handle installation on master/node
      super.setInstallations(installations);
    }
  }
}
