package io.snyk.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

public class SnykContext {
  private final FilePath workspace;
  private final Launcher launcher;
  private final EnvVars envVars;
  private final Run<?, ?> run;
  private final TaskListener taskListener;

  private SnykContext(
    FilePath workspace,
    Launcher launcher,
    EnvVars envVars,
    Run<?, ?> run,
    TaskListener taskListener
  ) {
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

  public static SnykContext forFreestyleProject(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
  throws IOException, InterruptedException {
    return new SnykContext(
      workspace,
      launcher,
      build.getEnvironment(listener),
      build,
      listener
    );
  }

  public static SnykContext forPipelineProject(StepContext context) throws IOException, InterruptedException {
    TaskListener listener = Optional.ofNullable(context.get(TaskListener.class))
      .orElseThrow(() -> new AbortException("Required context parameter 'TaskListener' is missing."));

    EnvVars envVars = Optional.ofNullable(context.get(EnvVars.class))
      .orElseThrow(() -> new AbortException("Required context parameter 'EnvVars' is missing."));

    FilePath workspace = Optional.ofNullable(context.get(FilePath.class))
      .orElseThrow(() -> new AbortException("Required context parameter 'FilePath' (workspace) is missing."));

    Launcher launcher = Optional.ofNullable(context.get(Launcher.class))
      .orElseThrow(() -> new AbortException("Required context parameter 'Launcher' is missing."));

    Run build = Optional.ofNullable(context.get(Run.class))
      .orElseThrow(() -> new AbortException("Required context parameter 'Run' is missing."));

    return new SnykContext(
      workspace,
      launcher,
      envVars,
      build,
      listener
    );
  }
}
