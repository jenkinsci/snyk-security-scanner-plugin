package io.snyk.tools;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import org.kohsuke.stapler.DataBoundConstructor;

public class SnykInstaller extends ToolInstaller {

  @DataBoundConstructor
  public SnykInstaller(String label) {
    super(label);
  }

  @Override
  public FilePath performInstallation(ToolInstallation toolInstallation, Node node, TaskListener taskListener) {
    return null;
  }
}
