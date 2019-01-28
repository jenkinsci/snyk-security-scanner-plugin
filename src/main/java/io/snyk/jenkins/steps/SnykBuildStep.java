package io.snyk.jenkins.steps;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.Result;
import hudson.security.ACL;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.SnykReportBuildAction;
import io.snyk.jenkins.credentials.SnykApiToken;
import io.snyk.jenkins.tools.SnykInstallation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.allOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.Util.fixEmptyAndTrim;
import static java.util.logging.Level.FINE;
import static java.util.logging.Logger.getLogger;

public class SnykBuildStep extends Builder {

  private static final Logger LOG = getLogger(SnykBuildStep.class.getName());

  private boolean failOnIssues = true;
  private boolean monitorProjectOnBuild = true;
  private Severity severity = Severity.LOW;
  private String snykTokenId;
  private String targetFile;
  private String organisation;
  private String projectName;
  private String snykInstallation;

  @DataBoundConstructor
  public SnykBuildStep() {
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
  public void setTargetFile(String targetFile) {
    this.targetFile = targetFile;
  }

  @SuppressWarnings("unused")
  public String getOrganisation() {
    return organisation;
  }

  @DataBoundSetter
  public void setOrganisation(String organisation) {
    this.organisation = organisation;
  }

  @SuppressWarnings("unused")
  public String getProjectName() {
    return projectName;
  }

  @DataBoundSetter
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  @SuppressWarnings("unused")
  public String getSnykInstallation() {
    return snykInstallation;
  }

  @DataBoundSetter
  public void setSnykInstallation(String snykInstallation) {
    this.snykInstallation = snykInstallation;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener log) throws InterruptedException, IOException {
    FilePath workspace = build.getWorkspace();
    if (workspace == null) {
      log.getLogger().println("Build agent is not connected");
      return false;
    }
    EnvVars env = build.getEnvironment(log);
    // build arguments
    ArgumentListBuilder args = new ArgumentListBuilder();

    // look for a snyk installation
    SnykInstallation installation = findSnykInstallation();
    if (installation != null) {
      // install if necessary
      Computer computer = Computer.currentComputer();
      Node node = computer != null ? computer.getNode() : null;
      if (node != null) {
        installation = installation.forNode(node, log);
        installation = installation.forEnvironment(env);
        String exe = installation.getSnykExecutable(launcher);
        if (exe == null) {
          log.getLogger().println("Can't retrieve the Snyk executable.");
          return false;
        }
        args.add(exe);
      } else {
        log.getLogger().println("Not in a build node");
        return false;
      }
    } else {
      // no snyk installation either, fall back to simple command
      args.add("snyk");
    }

    SnykApiToken snykApiToken = getSnykTokenCredential();
    if (snykApiToken == null) {
      log.getLogger().println("Snyk API token was not defined! Please configure the build properly");
      build.setResult(Result.FAILURE);
      return false;
    }
    env.put("SNYK_TOKEN", snykApiToken.getToken().getPlainText());
    env.overrideAll(build.getBuildVariables());

    args.add("test", "--json");
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
    } else {
      args.add("--project-name=" + workspace.getName());
    }

    FilePath snykReport = workspace.child("snyk_report.json");
    OutputStream output = snykReport.write();

    try {
      log.getLogger().println("Testing for any known vulnerabilities");
      log.getLogger().println("> " + args);
      int exitCode = launcher.launch()
                             .cmds(args)
                             .envs(env)
                             .stdout(output)
                             .quiet(true)
                             .pwd(workspace)
                             .join();
      boolean success = (!failOnIssues || exitCode == 0);
      build.setResult(success ? Result.SUCCESS : Result.FAILURE);

      if (installation != null) {
        generateSnykHtmlReport(build, launcher, log, installation.getReportExecutable(launcher));

        if (build.getActions(SnykReportBuildAction.class).size() <= 0) {
          build.addAction(new SnykReportBuildAction(build));
          ArtifactArchiver artifactArchiver = new ArtifactArchiver(workspace.getName() + "_snyk_report.html");
          artifactArchiver.perform(build, workspace, launcher, log);
        }
      }

      return success;
    } catch (IOException ex) {
      Util.displayIOException(ex, log);
      ex.printStackTrace(log.fatalError("Snyk command execution failed"));
      build.setResult(Result.FAILURE);
      return false;
    }
  }

  private SnykInstallation findSnykInstallation() {
    return Stream.of(((SnykBuildStepDescriptor) getDescriptor()).getInstallations())
                 .filter(installation -> installation.getName().equals(snykInstallation))
                 .findFirst().orElse(null);
  }

  private SnykApiToken getSnykTokenCredential() {
    return CredentialsMatchers.firstOrNull(lookupCredentials(SnykApiToken.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()),
                                           withId(snykTokenId));
  }

  private void generateSnykHtmlReport(AbstractBuild<?, ?> build, Launcher launcher, BuildListener log, String reportExecutable) throws IOException, InterruptedException {
    EnvVars env = build.getEnvironment(log);
    ArgumentListBuilder args = new ArgumentListBuilder();

    FilePath workspace = build.getWorkspace();
    if (workspace == null) {
      log.getLogger().println("Build agent is not connected");
      return;
    }

    FilePath snykReportJson = workspace.child("snyk_report.json");
    if (!snykReportJson.exists()) {
      log.getLogger().println("Snyk report json doesn't exist");
      return;
    }

    workspace.child("snyk_report.html").write("", "UTF-8");

    args.add(reportExecutable);
    args.add("-i", "snyk_report.json", "-o", workspace.getName() + "_snyk_report.html");
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
    } catch (IOException ex) {
      Util.displayIOException(ex, log);
      ex.printStackTrace(log.fatalError("Snyk-to-Html command execution failed"));
    }
  }

  @Extension
  @SuppressWarnings("unused")
  public static class SnykBuildStepDescriptor extends BuildStepDescriptor<Builder> {

    @CopyOnWrite
    private volatile SnykInstallation[] installations = new SnykInstallation[0];

    public SnykBuildStepDescriptor() {
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

    public FormValidation doCheckProjectName(@QueryParameter String value, @QueryParameter String monitorProjectOnBuild) {
      if (fixEmptyAndTrim(value) != null && "false".equals(fixEmptyAndTrim(monitorProjectOnBuild))) {
        return FormValidation.warning("Project name will be ignored, because the project is not monitored on build.");
      }
      return FormValidation.ok();
    }
  }
}
