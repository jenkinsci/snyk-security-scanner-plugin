package io.snyk.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.exception.SnykErrorException;
import io.snyk.jenkins.exception.SnykIssueException;
import io.snyk.jenkins.model.ObjectMapperHelper;
import io.snyk.jenkins.model.SnykMonitorResult;
import io.snyk.jenkins.model.SnykTestResult;
import io.snyk.jenkins.tools.SnykInstallation;
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.*;
import static io.snyk.jenkins.CLIArgs.buildArgumentList;
import static io.snyk.jenkins.config.SnykConstants.*;
import static java.util.stream.Collectors.joining;

public class SnykStepBuilder extends Builder implements SimpleBuildStep {

  private static final Logger LOG = LoggerFactory.getLogger(SnykStepBuilder.class.getName());

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
  public SnykStepBuilder() {
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
  public void perform(
    @Nonnull Run<?, ?> run,
    @Nonnull FilePath workspace,
    @Nonnull Launcher launcher,
    @Nonnull TaskListener listener
  ) throws InterruptedException, IOException {

  }

  @Override
  public boolean perform(@Nonnull AbstractBuild<?, ?> build, @Nonnull Launcher launcher, @Nonnull BuildListener log)
  throws SnykIssueException, SnykErrorException {
    int testExitCode = 0;
    Exception cause = null;

    try {
      FilePath workspace = Optional.ofNullable(build.getWorkspace()).orElseThrow(() -> new AbortException(
        "Build agent is not connected"));
      EnvVars envVars = build.getEnvironment(log);

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
      String snykExecutable = Optional.ofNullable(installation.getSnykExecutable(launcher))
        .orElseThrow(() -> new AbortException("Can't retrieve the Snyk executable."));

      SnykApiToken snykApiToken = Optional.ofNullable(getSnykTokenCredential(build))
        .orElseThrow(() -> new AbortException("Snyk API token with ID '" + snykTokenId + "' was not found. Please configure the build properly and retry."));
      envVars.put("SNYK_TOKEN", snykApiToken.getToken().getPlainText());

      envVars.overrideAll(build.getBuildVariables());

      // Workaround until we implement Step interface.
      getInstallationPath(node, installation).ifPresent(installationPath -> {
        LOG.info("Custom build tool path: '{}'", installationPath);
        envVars.put("PATH", installationPath);
      });

      FilePath snykTestReport = workspace.child(SNYK_TEST_REPORT_JSON);
      FilePath snykTestDebug = workspace.child(SNYK_TEST_REPORT_JSON + ".debug");

      ArgumentListBuilder argsForTestCommand = buildArgumentList(
        snykExecutable,
        "test",
        envVars,
        severity,
        targetFile,
        organisation,
        projectName,
        additionalArguments
      );

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

      Optional<String> monitorUri = monitorProjectOnBuild
        ? SnykMonitor.execute(
          workspace,
          launcher,
          envVars,
          log.getLogger(),
          snykExecutable,
          severity,
          targetFile,
          organisation,
          projectName,
          additionalArguments
        )
        : Optional.empty();

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
      if (ex instanceof IOException) {
        Util.displayIOException((IOException) ex, log);
      }
      ex.printStackTrace(log.fatalError("Snyk command execution failed"));
      cause = ex;
    }

    if (failOnIssues && testExitCode == 1) {
      throw new SnykIssueException();
    }
    if (failOnError && cause != null) {
      throw new SnykErrorException(cause.getMessage());
    }

    return true;
  }

  private Optional<String> getInstallationPath(Node node, SnykInstallation installation) {
    return Optional.ofNullable(node.getChannel())
      .flatMap(nodeChannel -> Optional.ofNullable(installation.getHome())
        .map(Util::fixEmptyAndTrim)
        .map(toolHome -> new FilePath(nodeChannel, toolHome))
        .map(toolPath -> {
          try {
            return toolPath.act(new CustomBuildToolPathCallable());
          } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
          }
        }));
  }

  private Optional<SnykInstallation> findSnykInstallation() {
    return Stream.of(((SnykStepBuilderDescriptor) getDescriptor()).getInstallations())
      .filter(installation -> installation.getName().equals(snykInstallation))
      .findFirst();
  }

  private SnykApiToken getSnykTokenCredential(@Nonnull AbstractBuild<?, ?> build) {
    return findCredentialById(snykTokenId, SnykApiToken.class, build);
  }

  @Extension
  @Symbol("snykSecurity")
  public static class SnykStepBuilderDescriptor extends BuildStepDescriptor<Builder> {

    @CopyOnWrite
    private volatile SnykInstallation[] installations = new SnykInstallation[0];

    public SnykStepBuilderDescriptor() {
      load();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Invoke Snyk Security task";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public SnykInstallation[] getInstallations() {
      return installations;
    }

    public void setInstallations(SnykInstallation... installations) {
      this.installations = installations;
      save();
    }

    public boolean hasInstallationsAvailable() {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "Available Snyk installations: {}",
          Arrays.stream(installations).map(SnykInstallation::getName).collect(joining(",", "[", "]"))
        );
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

    public ListBoxModel doFillSnykTokenIdItems(@AncestorInPath Item item, @QueryParameter String snykTokenId) {
      StandardListBoxModel model = new StandardListBoxModel();
      if (item == null) {
        Jenkins jenkins = Jenkins.get();
        if (!jenkins.hasPermission(Jenkins.ADMINISTER)) {
          return model.includeCurrentValue(snykTokenId);
        }
      } else {
        if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
          return model.includeCurrentValue(snykTokenId);
        }
      }
      return model.includeEmptyValue()
        .includeAs(ACL.SYSTEM, item, SnykApiToken.class)
        .includeCurrentValue(snykTokenId);
    }

    public FormValidation doCheckSeverity(@QueryParameter String value, @QueryParameter String additionalArguments) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(additionalArguments) == null) {
        return FormValidation.ok();
      }

      if (additionalArguments.contains("--severity-threshold")) {
        return FormValidation.warning(
          "Option '--severity-threshold' is overridden in additional arguments text area below.");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckSnykTokenId(@AncestorInPath Item item, @QueryParameter String value) {
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
        return FormValidation.error("Snyk API token is required.");
      }

      if (null == CredentialsMatchers.firstOrNull(
        lookupCredentials(SnykApiToken.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList()),
        anyOf(withId(value), CredentialsMatchers.instanceOf(SnykApiToken.class))
      )) {
        return FormValidation.error("Cannot find currently selected Snyk API token.");
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

    public FormValidation doCheckOrganisation(
      @QueryParameter String value,
      @QueryParameter String additionalArguments
    ) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(additionalArguments) == null) {
        return FormValidation.ok();
      }

      if (additionalArguments.contains("--org")) {
        return FormValidation.warning("Option '--org' is overridden in additional arguments text area below.");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckProjectName(
      @QueryParameter String value,
      @QueryParameter String monitorProjectOnBuild,
      @QueryParameter String additionalArguments
    ) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(monitorProjectOnBuild) == null) {
        return FormValidation.ok();
      }

      List<FormValidation> findings = new ArrayList<>(2);
      if ("false".equals(fixEmptyAndTrim(monitorProjectOnBuild))) {
        findings.add(FormValidation.warning(
          "Project name will be ignored, because the project is not monitored on build."));
      }
      if (fixNull(additionalArguments).contains("--project-name")) {
        findings.add(FormValidation.warning(
          "Option '--project-name' is overridden in additional arguments text area below."));
      }
      return FormValidation.aggregate(findings);
    }
  }
}
