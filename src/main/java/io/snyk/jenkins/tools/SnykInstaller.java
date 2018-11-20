package io.snyk.jenkins.tools;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class SnykInstaller extends ToolInstaller {

  @DataBoundConstructor
  public SnykInstaller(String label) {
    super(label);
  }

  @Override
  public FilePath performInstallation(ToolInstallation toolInstallation, Node node, TaskListener log) {
    FilePath expectedPath = preferredLocation(tool, node);

    log.getLogger().println(expectedPath.getRemote());

    return expectedPath;
  }

  @Extension
  public static final class SnykInstallerDescriptor extends ToolInstallerDescriptor<SnykInstaller> {

    @Override
    public String getDisplayName() {
      return "Install as global NPM package";
    }

    @Override
    public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
      return toolType == SnykInstallation.class;
    }
  }
}
