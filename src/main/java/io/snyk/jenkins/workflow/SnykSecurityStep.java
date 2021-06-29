package io.snyk.jenkins.workflow;

import hudson.*;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.*;
import io.snyk.jenkins.SnykStepBuilder.SnykStepBuilderDescriptor;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.exception.SnykErrorException;
import io.snyk.jenkins.exception.SnykIssueException;
import io.snyk.jenkins.tools.SnykInstallation;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;
import static hudson.Util.fixEmptyAndTrim;
import static io.snyk.jenkins.config.SnykConstants.SNYK_REPORT_HTML;

public class SnykSecurityStep extends Step implements SnykConfig {

  private static final Logger LOG = LoggerFactory.getLogger(SnykSecurityStep.class.getName());

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

  public static class Execution extends SynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1L;

    private final transient SnykSecurityStep snykSecurityStep;

    public Execution(@Nonnull SnykSecurityStep snykSecurityStep, @Nonnull StepContext context) {
      super(context);
      this.snykSecurityStep = snykSecurityStep;
    }

    @Override
    protected Void run() throws SnykIssueException, SnykErrorException {
      int testExitCode = 0;
      Exception cause = null;
      TaskListener log = null;

      try {
        log = getContext().get(TaskListener.class);
        if (log == null) {
          throw new AbortException("Required context parameter 'TaskListener' is missing.");
        }

        EnvVars envVars = getContext().get(EnvVars.class);
        if (envVars == null) {
          throw new AbortException("Required context parameter 'EnvVars' is missing.");
        }
        FilePath workspace = getContext().get(FilePath.class);
        if (workspace == null) {
          throw new AbortException("Required context parameter 'FilePath' (workspace) is missing.");
        }
        Launcher launcher = getContext().get(Launcher.class);
        if (launcher == null) {
          throw new AbortException("Required context parameter 'Launcher' is missing.");
        }
        Run build = getContext().get(Run.class);
        if (build == null) {
          throw new AbortException("Required context parameter 'Run' is missing.");
        }

        if (LOG.isTraceEnabled()) {
          LOG.trace("Configured EnvVars for build '{}'", build.getId());
          String envVarsAsString = envVars.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", ", "{", "}"));
          LOG.trace(envVarsAsString);
        }

        SnykInstallation installation = SnykInstallation.install(
          snykSecurityStep.snykInstallation,
          workspace,
          envVars,
          log
        );

        SnykApiToken snykApiToken = getSnykTokenCredential(build);
        if (snykApiToken == null) {
          throw new AbortException("Snyk API token with ID '" + snykSecurityStep.snykTokenId + "' was not found. Please configure the build properly and retry.");
        }
        envVars.put("SNYK_TOKEN", snykApiToken.getToken().getPlainText());

        testExitCode = SnykTest.testProject(workspace, launcher, installation, snykSecurityStep, envVars, log);

        if (snykSecurityStep.monitorProjectOnBuild) {
          SnykMonitor.monitorProject(workspace, launcher, installation, snykSecurityStep, envVars, log);
        }

        SnykToHTML.generateReport(
          build,
          workspace,
          launcher,
          installation,
          log
        );

        if (build.getActions(SnykReportBuildAction.class).isEmpty()) {
          build.addAction(new SnykReportBuildAction(build));
        }
        ArtifactArchiver artifactArchiver = new ArtifactArchiver(workspace.getName() + "_" + SNYK_REPORT_HTML);
        artifactArchiver.perform(build, workspace, launcher, log);
      } catch (IOException | InterruptedException | RuntimeException ex) {
        if (log != null) {
          if (ex instanceof IOException) {
            Util.displayIOException((IOException) ex, log);
          }
          ex.printStackTrace(log.fatalError("Snyk command execution failed"));
        }
        cause = ex;
      }

      if (snykSecurityStep.failOnIssues && testExitCode == 1) {
        throw new SnykIssueException();
      }
      if (snykSecurityStep.failOnError && cause != null) {
        throw new SnykErrorException(cause.getMessage());
      }

      return null;
    }

    private SnykApiToken getSnykTokenCredential(Run<?, ?> run) {
      return findCredentialById(snykSecurityStep.snykTokenId, SnykApiToken.class, run);
    }
  }
}
