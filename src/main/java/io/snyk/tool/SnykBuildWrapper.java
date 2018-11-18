package io.snyk.tool;

import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class SnykBuildWrapper extends BuildWrapper {

  private final String snykVersion;

  @DataBoundConstructor
  public SnykBuildWrapper(String snykVersion) {
    this.snykVersion = snykVersion;
  }

  private SnykInstallation getSnykInstallation() {
    return Stream.of(((DescriptorImpl) getDescriptor()).getInstallations())
                 .filter(snykInstallation -> snykInstallation.getName().equals(snykVersion))
                 .findFirst()
                 .orElse(null);
  }

  @Extension
  public static class DescriptorImpl extends BuildWrapperDescriptor {

    @CopyOnWrite
    private volatile SnykInstallation[] installations = new SnykInstallation[0];

    public DescriptorImpl() {
      load();
    }

    @NonNull
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
