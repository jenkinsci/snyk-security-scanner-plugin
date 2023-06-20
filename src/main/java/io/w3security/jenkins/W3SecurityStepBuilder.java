package io.w3security.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.w3security.jenkins.config.W3SecurityConfig;
import io.w3security.jenkins.credentials.W3SecurityApiToken;
import io.w3security.jenkins.exception.W3SecurityErrorException;
import io.w3security.jenkins.exception.W3SecurityIssueException;
import io.w3security.jenkins.tools.W3SecurityInstallation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.fixNull;
import static java.util.stream.Collectors.joining;

public class W3SecurityStepBuilder extends Builder implements SimpleBuildStep, W3SecurityConfig {

  private static final Logger LOG = LoggerFactory.getLogger(W3SecurityStepBuilder.class.getName());

  private boolean failOnIssues = true;
  private boolean failOnError = true;
  private boolean monitorProjectOnBuild = true;
  private Severity severity = Severity.LOW;
  private String w3securityTokenId;
  private String targetFile;
  private String organisation;
  private String projectName;
  private String w3securityInstallation;
  private String additionalArguments;

  @DataBoundConstructor
  public W3SecurityStepBuilder() {
    // called from stapler
  }

  @SuppressWarnings("unused")
  public boolean isFailOnIssues() {
    return failOnIssues;
  }

  @DataBoundSetter
  public void setFailOnIssues(boolean failOnIssues) {
    this.failOnIssues = failOnIssues;
  }

  @SuppressWarnings("unused")
  public boolean isFailOnError() {
    return failOnError;
  }

  @DataBoundSetter
  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  @SuppressWarnings("unused")
  public boolean isMonitorProjectOnBuild() {
    return monitorProjectOnBuild;
  }

  @DataBoundSetter
  public void setMonitorProjectOnBuild(boolean monitorProjectOnBuild) {
    this.monitorProjectOnBuild = monitorProjectOnBuild;
  }

  @SuppressWarnings("unused")
  public String getSeverity() {
    return severity != null ? severity.getSeverity() : null;
  }

  @DataBoundSetter
  public void setSeverity(String severity) {
    this.severity = Severity.getIfPresent(severity);
  }

  @SuppressWarnings("unused")
  public String getW3SecurityTokenId() {
    return w3securityTokenId;
  }

  @DataBoundSetter
  public void setW3SecurityTokenId(String w3securityTokenId) {
    this.w3securityTokenId = w3securityTokenId;
  }

  @SuppressWarnings("unused")
  public String getTargetFile() {
    return targetFile;
  }

  @DataBoundSetter
  public void setTargetFile(@CheckForNull String targetFile) {
    this.targetFile = fixEmptyAndTrim(targetFile);
  }

  @SuppressWarnings("unused")
  public String getOrganisation() {
    return organisation;
  }

  @DataBoundSetter
  public void setOrganisation(@CheckForNull String organisation) {
    this.organisation = fixEmptyAndTrim(organisation);
  }

  @SuppressWarnings("unused")
  public String getProjectName() {
    return projectName;
  }

  @DataBoundSetter
  public void setProjectName(@CheckForNull String projectName) {
    this.projectName = fixEmptyAndTrim(projectName);
  }

  @SuppressWarnings("unused")
  public String getW3SecurityInstallation() {
    return w3securityInstallation;
  }

  @DataBoundSetter
  public void setW3SecurityInstallation(String w3securityInstallation) {
    this.w3securityInstallation = w3securityInstallation;
  }

  @SuppressWarnings("unused")
  public String getAdditionalArguments() {
    return additionalArguments;
  }

  @DataBoundSetter
  public void setAdditionalArguments(@CheckForNull String additionalArguments) {
    this.additionalArguments = fixEmptyAndTrim(additionalArguments);
  }

  @Override
  public void perform(
      @Nonnull Run<?, ?> build,
      @Nonnull FilePath workspace,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener log) throws W3SecurityIssueException, W3SecurityErrorException {
    W3SecurityStepFlow.perform(this, () -> W3SecurityContext.forFreestyleProject(build, workspace, launcher, log));
  }

  @Extension
  public static class W3SecurityStepBuilderDescriptor extends BuildStepDescriptor<Builder> {

    @CopyOnWrite
    private volatile W3SecurityInstallation[] installations = new W3SecurityInstallation[0];

    public W3SecurityStepBuilderDescriptor() {
      load();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Invoke W3Security Security task";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public W3SecurityInstallation[] getInstallations() {
      return installations;
    }

    public void setInstallations(W3SecurityInstallation... installations) {
      this.installations = installations;
      save();
    }

    public boolean hasInstallationsAvailable() {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Available W3Security installations: {}",
            Arrays.stream(installations).map(W3SecurityInstallation::getName).collect(joining(",", "[", "]")));
      }

      return installations.length > 0;
    }

    public ListBoxModel doFillSeverityItems() {
      ListBoxModel model = new ListBoxModel();
      Stream.of(Severity.values())
          .map(Severity::getSeverity)
          .forEach(model::add);
      return model;
    }

    public ListBoxModel doFillW3SecurityTokenIdItems(@AncestorInPath Item item, @QueryParameter String w3securityTokenId) {
      StandardListBoxModel model = new StandardListBoxModel();
      if (item == null) {
        Jenkins jenkins = Jenkins.get();
        if (!jenkins.hasPermission(Jenkins.ADMINISTER)) {
          return model.includeCurrentValue(w3securityTokenId);
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return model.includeCurrentValue(w3securityTokenId);
        }
      }
      return model.includeEmptyValue()
          .includeAs(ACL.SYSTEM, item, W3SecurityApiToken.class)
          .includeCurrentValue(w3securityTokenId);
    }

    public FormValidation doCheckSeverity(@QueryParameter String value, @QueryParameter String additionalArguments) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(additionalArguments) == null) {
        return FormValidation.ok();
      }

      if (additionalArguments.contains("--severity-threshold")) {
        return FormValidation
            .warning("Option '--severity-threshold' is overridden in additional arguments text area below.");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckW3SecurityTokenId(@AncestorInPath Item item, @QueryParameter String value) {
      if (item == null) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
          return FormValidation.ok();
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return FormValidation.ok();
        }
      }
      if (fixEmptyAndTrim(value) == null) {
        return FormValidation.warningWithMarkup(
            "A W3Security API token is required. If you do not provide credentials, make sure to provide a <code>W3SECURITY_TOKEN</code> build environment variable.");
      }

      if (null == CredentialsMatchers.firstOrNull(
          lookupCredentials(W3SecurityApiToken.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList()),
          anyOf(withId(value), CredentialsMatchers.instanceOf(W3SecurityApiToken.class)))) {
        return FormValidation.error("Cannot find currently selected W3Security API token.");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckTargetFile(@QueryParameter String value, @QueryParameter String additionalArguments) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(additionalArguments) == null) {
        return FormValidation.ok();
      }

      if (additionalArguments.contains("--file")) {
        return FormValidation.warning("Option '--file' is overridden in additional arguments text area below.");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckOrganisation(@QueryParameter String value,
        @QueryParameter String additionalArguments) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(additionalArguments) == null) {
        return FormValidation.ok();
      }

      if (additionalArguments.contains("--org")) {
        return FormValidation.warning("Option '--org' is overridden in additional arguments text area below.");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckProjectName(@QueryParameter String value, @QueryParameter String monitorProjectOnBuild,
        @QueryParameter String additionalArguments) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(monitorProjectOnBuild) == null) {
        return FormValidation.ok();
      }

      List<FormValidation> findings = new ArrayList<>(2);
      if ("false".equals(fixEmptyAndTrim(monitorProjectOnBuild))) {
        findings.add(
            FormValidation.warning("Project name will be ignored, because the project is not monitored on build."));
      }
      if (fixNull(additionalArguments).contains("--project-name")) {
        findings.add(
            FormValidation.warning("Option '--project-name' is overridden in additional arguments text area below."));
      }
      return FormValidation.aggregate(findings);
    }
  }
}
