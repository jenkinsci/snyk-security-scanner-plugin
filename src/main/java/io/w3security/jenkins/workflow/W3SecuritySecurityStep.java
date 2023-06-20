package io.w3security.jenkins.workflow;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.w3security.jenkins.Severity;
import io.w3security.jenkins.W3SecurityContext;
import io.w3security.jenkins.W3SecurityStepBuilder;
import io.w3security.jenkins.W3SecurityStepBuilder.W3SecurityStepBuilderDescriptor;
import io.w3security.jenkins.W3SecurityStepFlow;
import io.w3security.jenkins.config.W3SecurityConfig;
import io.w3security.jenkins.exception.W3SecurityErrorException;
import io.w3security.jenkins.exception.W3SecurityIssueException;
import io.w3security.jenkins.tools.W3SecurityInstallation;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static hudson.Util.fixEmptyAndTrim;

public class W3SecuritySecurityStep extends Step implements W3SecurityConfig {

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
  public W3SecuritySecurityStep() {
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
  public StepExecution start(StepContext context) {
    return new W3SecuritySecurityStep.Execution(this, context);
  }

  public static class Execution extends SynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1L;

    private final transient W3SecurityConfig config;

    public Execution(@Nonnull W3SecurityConfig config, @Nonnull StepContext context) {
      super(context);
      this.config = config;
    }

    @Override
    protected Void run() throws W3SecurityIssueException, W3SecurityErrorException {
      W3SecurityStepFlow.perform(config, () -> W3SecurityContext.forPipelineProject(getContext()));
      return null;
    }
  }

  @Extension
  @Symbol("w3securitySecurity")
  public static class W3SecuritySecurityStepDescriptor extends StepDescriptor {

    private final W3SecurityStepBuilderDescriptor builderDescriptor;

    public W3SecuritySecurityStepDescriptor() {
      builderDescriptor = new W3SecurityStepBuilderDescriptor();
    }

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return new HashSet<>(Arrays.asList(EnvVars.class, FilePath.class, Launcher.class, Run.class, TaskListener.class));
    }

    @Override
    public String getFunctionName() {
      return "w3securitySecurity";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Invoke W3Security Security task";
    }

    @Override
    public String getConfigPage() {
      return getViewPage(W3SecurityStepBuilder.class, "config.jelly");
    }

    @SuppressWarnings("unused")
    public W3SecurityInstallation[] getInstallations() {
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
    public ListBoxModel doFillW3SecurityTokenIdItems(@AncestorInPath Item item, @QueryParameter String w3securityTokenId) {
      return builderDescriptor.doFillW3SecurityTokenIdItems(item, w3securityTokenId);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckSeverity(@QueryParameter String value, @QueryParameter String additionalArguments) {
      return builderDescriptor.doCheckSeverity(value, additionalArguments);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckW3SecurityTokenId(@AncestorInPath Item item, @QueryParameter String value) {
      return builderDescriptor.doCheckW3SecurityTokenId(item, value);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckTargetFile(@QueryParameter String value, @QueryParameter String additionalArguments) {
      return builderDescriptor.doCheckTargetFile(value, additionalArguments);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckOrganisation(@QueryParameter String value,
        @QueryParameter String additionalArguments) {
      return builderDescriptor.doCheckOrganisation(value, additionalArguments);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckProjectName(@QueryParameter String value, @QueryParameter String monitorProjectOnBuild,
        @QueryParameter String additionalArguments) {
      return builderDescriptor.doCheckProjectName(value, monitorProjectOnBuild, additionalArguments);
    }
  }
}
