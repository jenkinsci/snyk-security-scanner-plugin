package io.w3security.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import io.w3security.jenkins.command.Command;
import io.w3security.jenkins.command.CommandLine;
import io.w3security.jenkins.config.W3SecurityConfig;
import io.w3security.jenkins.tools.W3SecurityInstallation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

public class W3SecurityMonitor {

  public static void monitorProject(
      W3SecurityContext context,
      W3SecurityConfig config,
      W3SecurityInstallation installation,
      String w3securityToken) throws IOException, InterruptedException {
    PrintStream logger = context.getLogger();
    FilePath workspace = context.getWorkspace();
    Launcher launcher = context.getLauncher();
    EnvVars envVars = context.getEnvVars();

    ArgumentListBuilder command = CommandLine.asArgumentList(
        installation.getW3SecurityExecutable(launcher),
        Command.MONITOR,
        config,
        envVars);

    Map<String, String> commandEnvVars = CommandLine.asEnvVars(w3securityToken, envVars);

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
