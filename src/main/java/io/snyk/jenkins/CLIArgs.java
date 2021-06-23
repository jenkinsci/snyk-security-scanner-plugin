package io.snyk.jenkins;

import hudson.EnvVars;
import hudson.util.ArgumentListBuilder;

import javax.annotation.Nonnull;

import static hudson.Util.*;

public class CLIArgs {
  public static ArgumentListBuilder buildArgumentList(
    String snykExecutable,
    String snykCommand,
    @Nonnull EnvVars env,
    Severity severity,
    String targetFile,
    String organisation,
    String projectName,
    String additionalArguments
  ) {
    ArgumentListBuilder args = new ArgumentListBuilder(snykExecutable, snykCommand, "--json");

    if (severity != null && fixEmptyAndTrim(severity.getSeverity()) != null) {
      args.add("--severity-threshold=" + severity.getSeverity());
    }
    if (fixEmptyAndTrim(targetFile) != null) {
      args.add("--file=" + replaceMacro(targetFile, env));
    }
    if (fixEmptyAndTrim(organisation) != null) {
      args.add("--org=" + replaceMacro(organisation, env));
    }
    if (fixEmptyAndTrim(projectName) != null) {
      args.add("--project-name=" + replaceMacro(projectName, env));
    }
    if (fixEmptyAndTrim(additionalArguments) != null) {
      for (String addArg : tokenize(additionalArguments)) {
        if (fixEmptyAndTrim(addArg) != null) {
          args.add(replaceMacro(addArg, env));
        }
      }
    }

    return args;
  }
}
