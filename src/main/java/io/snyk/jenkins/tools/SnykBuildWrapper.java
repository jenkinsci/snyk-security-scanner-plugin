package io.snyk.jenkins.tools;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import static java.util.logging.Level.FINE;

public class SnykBuildWrapper extends BuildWrapper {

  private static final Logger LOG = Logger.getLogger(SnykBuildWrapper.class.getName());

  private final String snykVersion;

  @DataBoundConstructor
  public SnykBuildWrapper(String snykVersion) {
    this.snykVersion = snykVersion;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    SnykInstallation installation = getSnykInstallation();
    if (installation != null) {
      EnvVars env = build.getEnvironment(listener);
      env.overrideAll(build.getBuildVariables());

      // install if necessary
      Computer computer = Computer.currentComputer();
      if (computer != null && computer.getNode() != null) {
        installation = installation.forNode(computer.getNode(), listener)
                                   .forEnvironment(build.getEnvironment(listener));
      }
    }

    // apply Snyk binaries to PATH
    final SnykInstallation install = installation;
    return new Environment() {
      @Override
      public void buildEnvVars(Map<String, String> env) {
        if (install != null) {
          EnvVars envVars = new EnvVars();
          install.buildEnvVars(envVars);
          env.putAll(envVars);
        }
      }
    };
  }

  private SnykInstallation getSnykInstallation() {
    return Stream.of(((SnykBuildWrapperDescriptor) getDescriptor()).getInstallations())
                 .filter(snykInstallation -> snykInstallation.getName().equals(snykVersion))
                 .findFirst()
                 .orElse(null);
  }

  @Extension
  @SuppressWarnings({"WeakerAccess", "unused"})
  public static class SnykBuildWrapperDescriptor extends BuildWrapperDescriptor {

    @CopyOnWrite
    private volatile SnykInstallation[] installations = new SnykInstallation[0];

    public SnykBuildWrapperDescriptor() {
      load();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Set up Snyk security tools";
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> abstractProject) {
      return true;
    }

    public SnykInstallation[] getInstallations() {
      return installations;
    }

    public void setInstallations(SnykInstallation... installations) {
      this.installations = installations;
      save();
    }

    public boolean hasInstallationsAvailable() {
      if (LOG.isLoggable(FINE)) {
        LOG.log(FINE, "configured snyk installations: {0}", installations.length);
        for (SnykInstallation installation : installations) {
          LOG.log(FINE, "- details: {0}", installation);
        }
      }

      return installations.length > 0;
    }
  }
}
