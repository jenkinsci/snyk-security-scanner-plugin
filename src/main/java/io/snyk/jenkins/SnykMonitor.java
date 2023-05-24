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
import java.util.Map;

public class SnykMonitor {

  public static void monitorProject(
    SnykContext context,
    SnykConfig config,
    SnykInstallation installation,
    String snykToken
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

    Map<String, String> commandEnvVars = CommandLine.asEnvVars(snykToken, envVars);

    logger.println("Monitoring project...");
    logger.println("> " + command);

    Launcher.ProcStarter starter = launcher.launch();
    starter.cmds(command);
    starter.envs(commandEnvVars);
    starter.stdout(logger);
    starter.stderr(logger);
    starter.quiet(true);
    starter.pwd(workspace);

    int exitCode = starter.join();

    if (exitCode != 0) {
      String msg = "Failed to monitor project. (Exit Code: " + exitCode + ")";
      logger.println(msg);
      if (config.isFailOnError()) {
        throw new RuntimeException(msg);
      }
    }
  }
}
