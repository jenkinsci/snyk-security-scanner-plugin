package io.snyk.jenkins.steps;

import javax.annotation.Nonnull;
import java.io.IOException;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

public class SnykBuildStep extends Builder {

  @DataBoundConstructor
  public SnykBuildStep() {

  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    return super.perform(build, launcher, listener);
  }

  @Extension
  public static class SnykBuildStepDescriptor extends BuildStepDescriptor<Builder> {

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Invoke Snyk Security task";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    //public ListBoxModel doFillnullItems() {
    //  return new ListBoxModel();
    //}
  }
}
