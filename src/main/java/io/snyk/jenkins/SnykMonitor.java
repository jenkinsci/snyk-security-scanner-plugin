package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.command.Command;
import io.snyk.jenkins.command.CommandLine;
import io.snyk.jenkins.config.SnykConfig;
import io.snyk.jenkins.tools.SnykInstallation;

import java.io.IOException;
import java.io.PrintStream;

public class SnykMonitor {

  public static void monitorProject(
    SnykContext context,
    SnykConfig config,
    SnykInstallation installation
  ) throws IOException, InterruptedException {
    PrintStream logger = context.getLogger();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    EnvVars envVars = context.getEnvVars();

    ArgumentListBuilder command = CommandLine.asArgumentList(
      installation.getSnykExecutable(launcher),
      Command.MONITOR,
      config,
      envVars
    );

    logger.println("Monitoring project...");
    logger.println("> " + command);
    int exitCode = launcher.launch()
        .cmds(command)
        .envs(envVars)
        .stdout(logger)
        .stderr(logger)
        .quiet(true)
        .pwd(workspace)
        .join();

    if (exitCode != 0) {
      logger.println("Failed to monitor project. (Exit Code: " + exitCode + ")");
    }
  }
}
