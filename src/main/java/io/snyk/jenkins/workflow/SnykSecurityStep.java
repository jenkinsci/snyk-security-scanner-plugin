package io.snyk.jenkins.workflow;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.Severity;
import io.snyk.jenkins.SnykContext;
import io.snyk.jenkins.SnykStepBuilder;
import io.snyk.jenkins.SnykStepBuilder.SnykStepBuilderDescriptor;
import io.snyk.jenkins.SnykStepFlow;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.exception.SnykErrorException;
import io.snyk.jenkins.exception.SnykIssueException;
import io.snyk.jenkins.tools.SnykInstallation;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Serial;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static hudson.Util.fixEmptyAndTrim;

public class SnykSecurityStep extends Step implements SnykConfig {

  private boolean failOnIssues = true;
  private boolean failOnError = true;
  private boolean monitorProjectOnBuild = true;
  private Severity severity = Severity.LOW;
  private String snykTokenId;
  private String targetFile;
  private String organisation;
  private String projectName;
  private String snykInstallation;
  private String additionalArguments;

  @DataBoundConstructor
  public SnykSecurityStep() {
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
  public String getSnykTokenId() {
    return snykTokenId;
  }

  @DataBoundSetter
  public void setSnykTokenId(String snykTokenId) {
    this.snykTokenId = snykTokenId;
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
  public String getSnykInstallation() {
    return snykInstallation;
  }

  @DataBoundSetter
  public void setSnykInstallation(String snykInstallation) {
    this.snykInstallation = snykInstallation;
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
  public StepExecution start(StepContext context) {
    return new SnykSecurityStep.Execution(this, context);
  }

  public static class Execution extends SynchronousNonBlockingStepExecution<Void> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient SnykConfig config;

    public Execution(@Nonnull SnykConfig config, @Nonnull StepContext context) {
      super(context);
      this.config = config;
    }

    @Override
    protected Void run() throws SnykIssueException, SnykErrorException {
      SnykStepFlow.perform(config, () -> SnykContext.forPipelineProject(getContext()));
      return null;
    }
  }

  @Extension
  @Symbol("snykSecurity")
  public static class SnykSecurityStepDescriptor extends StepDescriptor {

    private final SnykStepBuilderDescriptor builderDescriptor;

    public SnykSecurityStepDescriptor() {
      builderDescriptor = new SnykStepBuilderDescriptor();
    }

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return new HashSet<>(Arrays.asList(EnvVars.class, FilePath.class, Launcher.class, Run.class, TaskListener.class));
    }

    @Override
    public String getFunctionName() {
      return "snykSecurity";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Invoke Snyk Security task";
    }

    @Override
    public String getConfigPage() {
      return getViewPage(SnykStepBuilder.class, "config.jelly");
    }

    @SuppressWarnings("unused")
    public SnykInstallation[] getInstallations() {
      return builderDescriptor.getInstallations();
    }

    @SuppressWarnings("unused")
    public boolean hasInstallationsAvailable() {
      return builderDescriptor.hasInstallationsAvailable();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillSeverityItems() {
      return builderDescriptor.doFillSeverityItems();
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillSnykTokenIdItems(@AncestorInPath Item item, @QueryParameter String snykTokenId) {
      return builderDescriptor.doFillSnykTokenIdItems(item, snykTokenId);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckSeverity(@QueryParameter String value, @QueryParameter String additionalArguments) {
      return builderDescriptor.doCheckSeverity(value, additionalArguments);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckSnykTokenId(@AncestorInPath Item item, @QueryParameter String value) {
      return builderDescriptor.doCheckSnykTokenId(item, value);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckTargetFile(@QueryParameter String value, @QueryParameter String additionalArguments) {
      return builderDescriptor.doCheckTargetFile(value, additionalArguments);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckOrganisation(@QueryParameter String value, @QueryParameter String additionalArguments) {
      return builderDescriptor.doCheckOrganisation(value, additionalArguments);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckProjectName(@QueryParameter String value, @QueryParameter String monitorProjectOnBuild, @QueryParameter String additionalArguments) {
      return builderDescriptor.doCheckProjectName(value, monitorProjectOnBuild, additionalArguments);
    }
  }
}
