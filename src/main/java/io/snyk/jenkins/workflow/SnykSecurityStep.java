package io.snyk.jenkins.workflow;

import hudson.*;
import hudson.model.*;
import hudson.tasks.ArtifactArchiver;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.Severity;
import io.snyk.jenkins.SnykReportBuildAction;
import io.snyk.jenkins.SnykStepBuilder;
import io.snyk.jenkins.SnykStepBuilder.SnykStepBuilderDescriptor;
import io.snyk.jenkins.SnykToHTML;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.exception.SnykErrorException;
import io.snyk.jenkins.exception.SnykIssueException;
import io.snyk.jenkins.model.ObjectMapperHelper;
import io.snyk.jenkins.model.SnykMonitorResult;
import io.snyk.jenkins.model.SnykTestResult;
import io.snyk.jenkins.tools.SnykInstallation;
import jenkins.model.Jenkins;
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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;
import static hudson.Util.*;
import static io.snyk.jenkins.config.SnykConstants.*;

public class SnykSecurityStep extends Step {

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
    public FormValidation doCheckOrganisation(
      @QueryParameter String value,
      @QueryParameter String additionalArguments
    ) {
      return builderDescriptor.doCheckOrganisation(value, additionalArguments);
    }

    @SuppressWarnings("unused")
    public FormValidation doCheckProjectName(
      @QueryParameter String value,
      @QueryParameter String monitorProjectOnBuild,
      @QueryParameter String additionalArguments
    ) {
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

    private <T> T fromContext(Class<T> serviceClass) throws AbortException {
      try {
        return Optional.ofNullable(getContext().get(serviceClass)).orElseThrow(() -> new AbortException(
          "Required context parameter '" + serviceClass.getName() + "' is missing."));
      } catch (InterruptedException | IOException e) {
        throw new RuntimeException("Failed to get context parameter '" + serviceClass.getName() + "'", e);
      }
    }

    @Override
    protected Void run() throws SnykIssueException, SnykErrorException {
      String snykInstallation = snykSecurityStep.snykInstallation;
      String snykTokenId = snykSecurityStep.snykTokenId;
      boolean monitorProjectOnBuild = snykSecurityStep.monitorProjectOnBuild;
      boolean failOnIssues = snykSecurityStep.failOnIssues;
      boolean failOnError = snykSecurityStep.failOnError;

      int testExitCode = 0;
      Exception cause = null;
      TaskListener log = null;

      try {
        log = fromContext(TaskListener.class);
        Run<?, ?> build = fromContext(Run.class);
        Launcher launcher = fromContext(Launcher.class);
        FilePath workspace = fromContext(FilePath.class);
        EnvVars envVars = fromContext(EnvVars.class);

        if (LOG.isTraceEnabled()) {
          LOG.trace("Configured EnvVars for build '{}'", build.getId());
          String envVarsAsString = envVars.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", ", "{", "}"));
          LOG.trace(envVarsAsString);
        }

        SnykInstallation installation = findSnykInstallation()
          .orElseThrow(() -> new AbortException("Snyk installation named '" + snykInstallation + "' was not found. Please configure the build properly and retry."));

        Node node = Optional.ofNullable(workspace.toComputer())
          .map(Computer::getNode)
          .orElseThrow(() -> new AbortException("Not running on a build node."));

        installation = installation.forNode(node, log);
        installation = installation.forEnvironment(envVars);
        String snykExecutable = Optional.ofNullable(installation.getSnykExecutable(launcher)).orElseThrow(() -> new AbortException(
          "Can't retrieve the Snyk executable."));

        SnykApiToken snykApiToken = Optional.ofNullable(getSnykTokenCredential(build))
          .orElseThrow(() -> new AbortException("Snyk API token with ID '" + snykTokenId + "' was not found. Please configure the build properly and retry."));
        envVars.put("SNYK_TOKEN", snykApiToken.getToken().getPlainText());

        FilePath snykTestReport = workspace.child(SNYK_TEST_REPORT_JSON);
        FilePath snykTestDebug = workspace.child(SNYK_TEST_REPORT_JSON + ".debug");

        ArgumentListBuilder argsForTestCommand = buildArgumentList(snykExecutable, "test", envVars);

        try (
          OutputStream snykTestOutput = snykTestReport.write();
          OutputStream snykTestDebugOutput = snykTestDebug.write()
        ) {
          log.getLogger().println("Testing for known issues...");
          log.getLogger().println("> " + argsForTestCommand);
          testExitCode = launcher.launch()
            .cmds(argsForTestCommand)
            .envs(envVars)
            .stdout(snykTestOutput)
            .stderr(snykTestDebugOutput)
            .quiet(true)
            .pwd(workspace)
            .join();
        }

        String snykTestReportAsString = snykTestReport.readToString();
        if (LOG.isTraceEnabled()) {
          LOG.trace("Job: '{}'", build);
          LOG.trace("Command line arguments: {}", argsForTestCommand);
          LOG.trace("Exit code: {}", testExitCode);
          LOG.trace("Command standard output: {}", snykTestReportAsString);
          LOG.trace("Command debug output: {}", snykTestDebug.readToString());
        }

        SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(snykTestReportAsString);
        if (snykTestResult == null) {
          throw new AbortException("Could not parse generated json report file.");
        }
        if (fixEmptyAndTrim(snykTestResult.error) != null) {
          throw new AbortException("Error result: " + snykTestResult.error);
        }
        if (testExitCode >= 2) {
          throw new AbortException("An error occurred. Exit code is " + testExitCode);
        }
        if (!snykTestResult.ok) {
          log.getLogger().println("Vulnerabilities found!");
          log.getLogger().printf(
            "Result: %s known vulnerabilities | %s dependencies%n",
            snykTestResult.uniqueCount,
            snykTestResult.dependencyCount
          );
        }

        String monitorUri = "";
        if (monitorProjectOnBuild) {
          FilePath snykMonitorReport = workspace.child(SNYK_MONITOR_REPORT_JSON);
          FilePath snykMonitorDebug = workspace.child(SNYK_MONITOR_REPORT_JSON + ".debug");

          ArgumentListBuilder argsForMonitorCommand = buildArgumentList(snykExecutable, "monitor", envVars);

          log.getLogger().println("Remember project for continuous monitoring...");
          log.getLogger().println("> " + argsForMonitorCommand);

          int monitorExitCode;
          try (
            OutputStream snykMonitorOutput = snykMonitorReport.write();
            OutputStream snykMonitorDebugOutput = snykMonitorDebug.write()
          ) {
            monitorExitCode = launcher.launch()
              .cmds(argsForMonitorCommand)
              .envs(envVars)
              .stdout(snykMonitorOutput)
              .stderr(snykMonitorDebugOutput)
              .quiet(true)
              .pwd(workspace)
              .join();
          }
          String snykMonitorReportAsString = snykMonitorReport.readToString();
          if (monitorExitCode != 0) {
            log.getLogger().println("Warning: 'snyk monitor' was not successful. Exit code: " + monitorExitCode);
            log.getLogger().println(snykMonitorReportAsString);
          }

          if (LOG.isTraceEnabled()) {
            LOG.trace("Command line arguments: {}", argsForMonitorCommand);
            LOG.trace("Exit code: {}", monitorExitCode);
            LOG.trace("Command standard output: {}", snykMonitorReportAsString);
            LOG.trace("Command debug output: {}", snykMonitorDebug.readToString());
          }

          SnykMonitorResult snykMonitorResult = ObjectMapperHelper.unmarshallMonitorResult(snykMonitorReportAsString);
          if (snykMonitorResult != null && fixEmptyAndTrim(snykMonitorResult.uri) != null) {
            log.getLogger().println("Explore the snapshot at " + snykMonitorResult.uri);
          }
        }

        SnykToHTML.generateReport(
          build,
          workspace,
          launcher,
          log,
          installation.getReportExecutable(launcher),
          monitorUri
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

      if (failOnIssues && testExitCode == 1) {
        throw new SnykIssueException();
      }
      if (failOnError && cause != null) {
        throw new SnykErrorException(cause.getMessage());
      }

      return null;
    }

    private Optional<SnykInstallation> findSnykInstallation() {
      SnykStepBuilderDescriptor descriptor = Jenkins.get().getDescriptorByType(SnykStepBuilderDescriptor.class);
      return Stream.of(descriptor.getInstallations())
        .filter(installation -> installation.getName().equals(snykSecurityStep.snykInstallation))
        .findFirst();
    }

    private SnykApiToken getSnykTokenCredential(Run<?, ?> run) {
      return findCredentialById(snykSecurityStep.snykTokenId, SnykApiToken.class, run);
    }

    ArgumentListBuilder buildArgumentList(String snykExecutable, String snykCommand, @Nonnull EnvVars env) {
      ArgumentListBuilder args = new ArgumentListBuilder(snykExecutable, snykCommand, "--json");

      if (fixEmptyAndTrim(snykSecurityStep.severity.getSeverity()) != null) {
        args.add("--severity-threshold=" + snykSecurityStep.severity.getSeverity());
      }
      if (fixEmptyAndTrim(snykSecurityStep.targetFile) != null) {
        args.add("--file=" + replaceMacro(snykSecurityStep.targetFile, env));
      }
      if (fixEmptyAndTrim(snykSecurityStep.organisation) != null) {
        args.add("--org=" + replaceMacro(snykSecurityStep.organisation, env));
      }
      if (fixEmptyAndTrim(snykSecurityStep.projectName) != null) {
        args.add("--project-name=" + replaceMacro(snykSecurityStep.projectName, env));
      }
      if (fixEmptyAndTrim(snykSecurityStep.additionalArguments) != null) {
        for (String addArg : tokenize(snykSecurityStep.additionalArguments)) {
          if (fixEmptyAndTrim(addArg) != null) {
            args.add(replaceMacro(addArg, env));
          }
        }
      }

      return args;
    }
  }
}
