package io.snyk.jenkins;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.tools.Platform;
import io.snyk.jenkins.tools.SnykInstallation;
import io.snyk.jenkins.transform.ReportConverter;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.allOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.fixEmptyAndTrim;
import static hudson.Util.fixNull;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

public class SnykStepBuilder extends Builder implements SimpleBuildStep {

  private static final Logger LOG = LoggerFactory.getLogger(SnykStepBuilder.class.getName());
  private static final String SNYK_REPORT_HTML = "snyk_report.html";
  private static final String SNYK_TEST_REPORT_JSON = "snyk_report.json";
  private static final String SNYK_MONITOR_REPORT_JSON = "snyk_monitor_report.json";

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
  public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener log) throws InterruptedException, IOException {
    EnvVars env = build.getEnvironment(log);

    // look for a snyk installation
    SnykInstallation installation = findSnykInstallation();
    String snykExecutable;
    Platform platform;
    if (installation == null) {
      log.getLogger().println("Snyk installation named '" + snykInstallation + "' was not found. Please configure the build properly and retry.");
      build.setResult(Result.FAILURE);
      return;
    }

    // install if necessary
    Computer computer = workspace.toComputer();
    Node node = computer != null ? computer.getNode() : null;
    platform = Platform.of(node);
    if (node == null) {
      log.getLogger().println("Not running on a build node.");
      build.setResult(Result.FAILURE);
      return;
    }

    installation = installation.forNode(node, log);
    installation = installation.forEnvironment(env);
    snykExecutable = installation.getSnykExecutable(launcher, platform);

    if (snykExecutable == null) {
      log.getLogger().println("Can't retrieve the Snyk executable.");
      build.setResult(Result.FAILURE);
      return;
    }

    SnykApiToken snykApiToken = getSnykTokenCredential();
    if (snykApiToken == null) {
      log.getLogger().println("Snyk API token with ID '" + snykTokenId + "' was not found. Please configure the build properly and retry.");
      build.setResult(Result.FAILURE);
      return;
    }

    env.put("SNYK_TOKEN", snykApiToken.getToken().getPlainText());
    env.overrideAll(build.getEnvironment(log));

    FilePath snykTestReport = workspace.child(SNYK_TEST_REPORT_JSON);
    OutputStream snykTestOutput = snykTestReport.write();
    ArgumentListBuilder argsForTestCommand = buildArgumentList(snykExecutable, "test");

    try {
      log.getLogger().println("Testing for known issues...");
      log.getLogger().println("> " + argsForTestCommand);
      int exitCode = launcher.launch()
                             .cmds(argsForTestCommand)
                             .envs(env)
                             .stdout(snykTestOutput)
                             .quiet(true)
                             .pwd(workspace)
                             .join();
      boolean success = (!failOnIssues || exitCode == 0);
      build.setResult(success ? Result.SUCCESS : Result.FAILURE);

      if (LOG.isTraceEnabled()) {
        LOG.trace("Job: '{}'", build);
        LOG.trace("Command line arguments: {}", argsForTestCommand);
        LOG.trace("Exit code: {}", exitCode);
        LOG.trace("Command output: {}", snykTestReport.readToString());
      }

      JSONObject snykTestReportJson = JSONObject.fromObject(snykTestReport.readToString());
      // exit on cli error immediately
      if (snykTestReportJson.has("error")) {
        log.getLogger().println("Error result: " + snykTestReportJson.getString("error"));
        build.setResult(Result.FAILURE);
        return;
      }

      if (snykTestReportJson.has("summary") && snykTestReportJson.has("uniqueCount")) {
        log.getLogger().println(format("Result: %s known issues | %s", snykTestReportJson.getString("uniqueCount"), snykTestReportJson.getString("summary")));
      }

      String monitorUri = "";
      if (monitorProjectOnBuild) {
        FilePath snykMonitorReport = workspace.child(SNYK_MONITOR_REPORT_JSON);
        OutputStream snykMonitorOutput = snykMonitorReport.write();
        ArgumentListBuilder argsForMonitorCommand = buildArgumentList(snykExecutable, "monitor");

        log.getLogger().println("Remember project for continuous monitoring...");
        log.getLogger().println("> " + argsForMonitorCommand);
        exitCode = launcher.launch()
                           .cmds(argsForMonitorCommand)
                           .envs(env)
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

      generateSnykHtmlReport(build, workspace, launcher, log, installation.getReportExecutable(launcher, platform), monitorUri);

      if (build.getActions(SnykReportBuildAction.class).isEmpty()) {
        build.addAction(new SnykReportBuildAction(build));
        ArtifactArchiver artifactArchiver = new ArtifactArchiver(workspace.getName() + "_" + SNYK_REPORT_HTML);
        artifactArchiver.perform(build, workspace, launcher, log);
      }
    } catch (IOException ex) {
      Util.displayIOException(ex, log);
      ex.printStackTrace(log.fatalError("Snyk command execution failed"));
      build.setResult(Result.FAILURE);
    }
  }

  private SnykInstallation findSnykInstallation() {
    return Stream.of(((SnykStepBuilderDescriptor) getDescriptor()).getInstallations())
                 .filter(installation -> installation.getName().equals(snykInstallation))
                 .findFirst().orElse(null);
  }

  private SnykApiToken getSnykTokenCredential() {
    return CredentialsMatchers.firstOrNull(lookupCredentials(SnykApiToken.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()),
                                           withId(snykTokenId));
  }

  private ArgumentListBuilder buildArgumentList(String snykExecutable, String snykCommand) {
    ArgumentListBuilder args = new ArgumentListBuilder(snykExecutable, snykCommand, "--json");

    if (fixEmptyAndTrim(severity.getSeverity()) != null) {
      args.add("--severity-threshold=" + severity.getSeverity());
    }
    if (fixEmptyAndTrim(targetFile) != null) {
      args.add("--file=" + targetFile);
    }
    if (fixEmptyAndTrim(organisation) != null) {
      args.add("--org=" + organisation);
    }
    if (fixEmptyAndTrim(projectName) != null) {
      args.add("--project-name=" + projectName);
    }
    if (fixEmptyAndTrim(additionalArguments) != null) {
      args.add(additionalArguments);
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

    @SuppressWarnings("unused")
    public boolean hasInstallationsAvailable() {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Available Snyk installations: {}",
                  Arrays.stream(installations).map(SnykInstallation::getName).collect(joining(",", "[", "]")));
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
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && !jenkins.hasPermission(Jenkins.ADMINISTER)) {
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
        return FormValidation.warning("Option '--severity-threshold' is overridden in additional arguments text area below.");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckSnykTokenId(@QueryParameter String value) {
      if (fixEmptyAndTrim(value) == null) {
        return FormValidation.error("Snyk API token is required.");
      } else {
        if (null == CredentialsMatchers.firstOrNull(lookupCredentials(SnykApiToken.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()),
                                                    allOf(withId(value), CredentialsMatchers.instanceOf(SnykApiToken.class)))) {
          return FormValidation.error("Cannot find currently selected Snyk API token.");
        }
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

    public FormValidation doCheckOrganisation(@QueryParameter String value, @QueryParameter String additionalArguments) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(additionalArguments) == null) {
        return FormValidation.ok();
      }

      if (additionalArguments.contains("--org")) {
        return FormValidation.warning("Option '--org' is overridden in additional arguments text area below.");
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckProjectName(@QueryParameter String value, @QueryParameter String monitorProjectOnBuild, @QueryParameter String additionalArguments) {
      if (fixEmptyAndTrim(value) == null || fixEmptyAndTrim(monitorProjectOnBuild) == null) {
        return FormValidation.ok();
      }

      List<FormValidation> findings = new ArrayList<>(2);
      if ("false".equals(fixEmptyAndTrim(monitorProjectOnBuild))) {
        findings.add(FormValidation.warning("Project name will be ignored, because the project is not monitored on build."));
      }
      if (fixNull(additionalArguments).contains("--project-name")) {
        findings.add(FormValidation.warning("Option '--project-name' is overridden in additional arguments text area below."));
      }
      return FormValidation.aggregate(findings);
    }
  }
}
