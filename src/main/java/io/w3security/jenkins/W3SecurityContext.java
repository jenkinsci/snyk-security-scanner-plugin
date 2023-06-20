package io.w3security.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

public class W3SecurityContext {
  private final FilePath workspace;
  private final Launcher launcher;
  private final EnvVars envVars;
  private final Run<?, ?> run;
  private final TaskListener taskListener;

  private W3SecurityContext(
      FilePath workspace,
      Launcher launcher,
      EnvVars envVars,
      Run<?, ?> run,
      TaskListener taskListener) {
    this.workspace = workspace;
    this.launcher = launcher;
    this.envVars = envVars;
    this.run = run;
    this.taskListener = taskListener;
  }

  public FilePath getWorkspace() {
    return workspace;
  }

  public Launcher getLauncher() {
    return launcher;
  }

  public EnvVars getEnvVars() {
    return envVars;
  }

  public Run<?, ?> getRun() {
    return run;
  }

  public TaskListener getTaskListener() {
    return taskListener;
  }

  public PrintStream getLogger() {
    return taskListener.getLogger();
  }

  public static W3SecurityContext forFreestyleProject(Run<?, ?> build, FilePath workspace, Launcher launcher,
      TaskListener listener) {
    try {
      return new W3SecurityContext(
          workspace,
          launcher,
          build.getEnvironment(listener),
          build,
          listener);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static W3SecurityContext forPipelineProject(StepContext context) {
    try {
      TaskListener listener = Optional.ofNullable(context.get(TaskListener.class))
          .orElseThrow(() -> new RuntimeException("Required context parameter 'TaskListener' is missing."));

      EnvVars envVars = Optional.ofNullable(context.get(EnvVars.class))
          .orElseThrow(() -> new RuntimeException("Required context parameter 'EnvVars' is missing."));

      FilePath workspace = Optional.ofNullable(context.get(FilePath.class))
          .orElseThrow(() -> new RuntimeException("Required context parameter 'FilePath' (workspace) is missing."));

      Launcher launcher = Optional.ofNullable(context.get(Launcher.class))
          .orElseThrow(() -> new RuntimeException("Required context parameter 'Launcher' is missing."));

      Run<?, ?> build = Optional.ofNullable(context.get(Run.class))
          .orElseThrow(() -> new RuntimeException("Required context parameter 'Run' is missing."));

      return new W3SecurityContext(
          workspace,
          launcher,
          envVars,
          build,
          listener);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
