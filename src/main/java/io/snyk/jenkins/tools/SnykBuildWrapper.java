package io.snyk.jenkins.tools;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.stream.Stream;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class SnykBuildWrapper extends BuildWrapper {

  private final String snykVersion;

  @DataBoundConstructor
  public SnykBuildWrapper(String snykVersion) {
    this.snykVersion = snykVersion;
  }

  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    SnykInstallation installation = getSnykInstallation();
    if (installation != null) {
      listener.getLogger().println("Installation is not null");
      // install if necessary
      installation = installation.forNode(Computer.currentComputer().getNode(), listener).forEnvironment(build.getEnvironment(listener));
    }

    final SnykInstallation install = installation;
    listener.getLogger().println(install.getHome());

    return super.setUp(build, launcher, listener);
  }

  private SnykInstallation getSnykInstallation() {
    return Stream.of(((SnykBuildWrapperDescriptor) getDescriptor()).getInstallations())
                 .filter(snykInstallation -> snykInstallation.getName().equals(snykVersion))
                 .findFirst()
                 .orElse(null);
  }

  @Extension
  public static class SnykBuildWrapperDescriptor extends BuildWrapperDescriptor {

    @CopyOnWrite
    private volatile SnykInstallation[] installations = new SnykInstallation[0];

    public SnykBuildWrapperDescriptor() {
      load();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Snyk";
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> abstractProject) {
      return true;
    }

    SnykInstallation[] getInstallations() {
      return installations;
    }

    void setInstallations(SnykInstallation... installations) {
      this.installations = installations;
      save();
    }
  }
}
