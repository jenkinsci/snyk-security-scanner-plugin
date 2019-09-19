package io.snyk.jenkins.workflow;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.ArtifactArchiver;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.Severity;
import io.snyk.jenkins.SnykReportBuildAction;
import io.snyk.jenkins.SnykStepBuilder;
import io.snyk.jenkins.SnykStepBuilder.SnykStepBuilderDescriptor;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.tools.SnykInstallation;
import io.snyk.jenkins.transform.ReportConverter;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.replaceMacro;
import static hudson.Util.tokenize;
import static io.snyk.jenkins.config.SnykConstants.SNYK_MONITOR_REPORT_JSON;
import static io.snyk.jenkins.config.SnykConstants.SNYK_REPORT_HTML;
import static io.snyk.jenkins.config.SnykConstants.SNYK_TEST_REPORT_JSON;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SnykSecurityStep extends Step {

  private static final Logger LOG = LoggerFactory.getLogger(SnykSecurityStep.class.getName());

  private boolean failOnIssues = true;
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
    public FormValidation doCheckSnykTokenId(@QueryParameter String value) {
      return builderDescriptor.doCheckSnykTokenId(value);
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
    protected Void run() throws Exception {
      EnvVars envVars = getContext().get(EnvVars.class);
      if (envVars == null) {
        LOG.error("Required context parameter 'EnvVars' is missing.");
        return null;
      }
      FilePath workspace = getContext().get(FilePath.class);
      if (workspace == null) {
        LOG.error("Required context parameter 'FilePath' (workspace) is missing.");
        return null;
      }
      Launcher launcher = getContext().get(Launcher.class);
      if (launcher == null) {
        LOG.error("Required context parameter 'Launcher' is missing.");
        return null;
      }
      Run build = getContext().get(Run.class);
      if (build == null) {
        LOG.error("Required context parameter 'Run' is missing.");
        return null;
      }
      TaskListener log = getContext().get(TaskListener.class);
      if (log == null) {
        LOG.error("Required context parameter 'TaskListener' is missing.");
        return null;
      }

      if (LOG.isTraceEnabled()) {
        LOG.trace("Configured EnvVars for build '{}'", build.getId());
        String envVarsAsString = envVars.entrySet().stream()
                                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                                        .collect(Collectors.joining(", ", "{", "}"));
        LOG.trace(envVarsAsString);
      }

      // look for a snyk installation
      SnykInstallation installation = findSnykInstallation();
      String snykExecutable;
      if (installation == null) {
        log.getLogger().println("Snyk installation named '" + snykSecurityStep.snykInstallation + "' was not found. Please configure the build properly and retry.");
        build.setResult(Result.FAILURE);
        return null;
      }

      // install if necessary
      Computer computer = workspace.toComputer();
      Node node = computer != null ? computer.getNode() : null;
      if (node == null) {
        log.getLogger().println("Not running on a build node.");
        build.setResult(Result.FAILURE);
        return null;
      }

      installation = installation.forNode(node, log);
      installation = installation.forEnvironment(envVars);
      snykExecutable = installation.getSnykExecutable(launcher);

      if (snykExecutable == null) {
        log.getLogger().println("Can't retrieve the Snyk executable.");
        build.setResult(Result.FAILURE);
        return null;
      }

      SnykApiToken snykApiToken = getSnykTokenCredential();
      if (snykApiToken == null) {
        log.getLogger().println("Snyk API token with ID '" + snykSecurityStep.snykTokenId + "' was not found. Please configure the build properly and retry.");
        build.setResult(Result.FAILURE);
        return null;
      }
      envVars.put("SNYK_TOKEN", snykApiToken.getToken().getPlainText());

      FilePath snykTestReport = workspace.child(SNYK_TEST_REPORT_JSON);
      OutputStream snykTestOutput = snykTestReport.write();
      ArgumentListBuilder argsForTestCommand = buildArgumentList(snykExecutable, "test", envVars);

      try {
        log.getLogger().println("Testing for known issues...");
        log.getLogger().println("> " + argsForTestCommand);
        int exitCode = launcher.launch()
                               .cmds(argsForTestCommand)
                               .envs(envVars)
                               .stdout(snykTestOutput)
                               .quiet(true)
                               .pwd(workspace)
                               .join();
        boolean result = (!snykSecurityStep.failOnIssues || exitCode == 0);
        build.setResult(result ? Result.SUCCESS : Result.FAILURE);

        if (LOG.isTraceEnabled()) {
          LOG.trace("Command line arguments: {}", argsForTestCommand);
          LOG.trace("Exit code: {}", exitCode);
          LOG.trace("Command output: {}", snykTestReport.readToString());
        }

        JSONObject snykTestReportJson = JSONObject.fromObject(snykTestReport.readToString());
        // exit on cli error immediately
        if (snykTestReportJson.has("error")) {
          log.getLogger().println("Error result: " + snykTestReportJson.getString("error"));
          build.setResult(Result.FAILURE);
          return null;
        }

        if (snykTestReportJson.has("summary") && snykTestReportJson.has("uniqueCount")) {
          log.getLogger().println(format("Result: %s known issues | %s", snykTestReportJson.getString("uniqueCount"), snykTestReportJson.getString("summary")));
        }

        String monitorUri = "";
        if (snykSecurityStep.monitorProjectOnBuild) {
          FilePath snykMonitorReport = workspace.child(SNYK_MONITOR_REPORT_JSON);
          OutputStream snykMonitorOutput = snykMonitorReport.write();
          ArgumentListBuilder argsForMonitorCommand = buildArgumentList(snykExecutable, "monitor", envVars);

          log.getLogger().println("Remember project for continuous monitoring...");
          log.getLogger().println("> " + argsForMonitorCommand);
          exitCode = launcher.launch()
                             .cmds(argsForMonitorCommand)
                             .envs(envVars)
                             .stdout(snykMonitorOutput)
                             .quiet(true)
                             .pwd(workspace)
                             .join();
          if (exitCode != 0) {
            log.getLogger().println("Warning: 'snyk monitor' was not successful. Exit code: " + exitCode);
            log.getLogger().println(snykMonitorReport.readToString());
          }

          if (LOG.isTraceEnabled()) {
            LOG.trace("Command line arguments: {}", argsForMonitorCommand);
            LOG.trace("Exit code: {}", exitCode);
            LOG.trace("Command output: {}", snykMonitorReport.readToString());
          }

          JSONObject snykMonitorReportJson = JSONObject.fromObject(snykMonitorReport.readToString());
          if (snykMonitorReportJson.has("uri")) {
            monitorUri = snykMonitorReportJson.getString("uri");
          }
        }

        generateSnykHtmlReport(build, workspace, launcher, log, installation.getReportExecutable(launcher), monitorUri);

        if (build.getActions(SnykReportBuildAction.class).isEmpty()) {
          build.addAction(new SnykReportBuildAction(build));
        }
        ArtifactArchiver artifactArchiver = new ArtifactArchiver(workspace.getName() + "_" + SNYK_REPORT_HTML);
        artifactArchiver.perform(build, workspace, launcher, log);
      } catch (IOException ex) {
        Util.displayIOException(ex, log);
        ex.printStackTrace(log.fatalError("Snyk command execution failed"));
        build.setResult(Result.FAILURE);
        return null;
      }

      if (snykSecurityStep.failOnIssues && Result.FAILURE.equals(build.getResult())) {
        throw new FlowInterruptedException(Result.FAILURE, new FoundIssuesCause());
      }

      return null;
    }

    private SnykInstallation findSnykInstallation() {
      SnykStepBuilderDescriptor descriptor = Jenkins.getInstance().getDescriptorByType(SnykStepBuilderDescriptor.class);
      return Stream.of(descriptor.getInstallations())
                   .filter(installation -> installation.getName().equals(snykSecurityStep.snykInstallation))
                   .findFirst().orElse(null);
    }

    private SnykApiToken getSnykTokenCredential() {
      return CredentialsMatchers.firstOrNull(lookupCredentials(SnykApiToken.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()),
                                             withId(snykSecurityStep.snykTokenId));
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

    private void generateSnykHtmlReport(Run<?, ?> build, @Nonnull FilePath workspace, Launcher launcher, TaskListener log, String reportExecutable, String monitorUri) throws IOException, InterruptedException {
      EnvVars env = build.getEnvironment(log);
      ArgumentListBuilder args = new ArgumentListBuilder();

      FilePath snykReportJson = workspace.child(SNYK_TEST_REPORT_JSON);
      if (!snykReportJson.exists()) {
        log.getLogger().println("Snyk report json doesn't exist");
        return;
      }

      workspace.child(SNYK_REPORT_HTML).write("", UTF_8.name());

      args.add(reportExecutable);
      args.add("-i", SNYK_TEST_REPORT_JSON, "-o", SNYK_REPORT_HTML);
      try {
        int exitCode = launcher.launch()
                               .cmds(args)
                               .envs(env)
                               .quiet(true)
                               .pwd(workspace)
                               .join();
        boolean success = exitCode == 0;
        if (!success) {
          log.getLogger().println("Generating Snyk html report was not successful");
        }
        String reportWithInlineCSS = workspace.child(SNYK_REPORT_HTML).readToString();
        String modifiedHtmlReport = ReportConverter.getInstance().modifyHeadSection(reportWithInlineCSS);
        String finalHtmlReport = ReportConverter.getInstance().injectMonitorLink(modifiedHtmlReport, monitorUri);
        workspace.child(workspace.getName() + "_" + SNYK_REPORT_HTML).write(finalHtmlReport, UTF_8.name());
      } catch (IOException ex) {
        Util.displayIOException(ex, log);
        ex.printStackTrace(log.fatalError("Snyk-to-Html command execution failed"));
      }
    }
  }
}
