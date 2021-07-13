package io.snyk.jenkins.command;

import hudson.EnvVars;
import hudson.Util;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.config.SnykConfig;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public class CommandLine {
  public static ArgumentListBuilder asArgumentList(String executablePath, Command command, SnykConfig config, EnvVars env) {
    Function<String, String> replaceMacroWithEnv = str -> Util.replaceMacro(str, env);
    ArgumentListBuilder args = new ArgumentListBuilder(executablePath, command.commandName());

    Optional.ofNullable(config.getSeverity())
      .map(Util::fixEmptyAndTrim)
      .map(replaceMacroWithEnv)
      .ifPresent(value -> args.add("--severity-threshold=" + value));

    Optional.ofNullable(config.getTargetFile())
      .map(Util::fixEmptyAndTrim)
      .map(replaceMacroWithEnv)
      .ifPresent(value -> args.add("--file=" + value));

    Optional.ofNullable(config.getOrganisation())
      .map(Util::fixEmptyAndTrim)
      .map(replaceMacroWithEnv)
      .ifPresent(value -> args.add("--org=" + value));

    Optional.ofNullable(config.getProjectName())
      .map(Util::fixEmptyAndTrim)
      .map(replaceMacroWithEnv)
      .ifPresent(value -> args.add("--project-name=" + value));

    Optional.ofNullable(config.getAdditionalArguments())
      .map(Util::fixEmptyAndTrim)
      .map(Util::tokenize)
      .ifPresent(values -> Arrays.stream(values)
        .map(Util::fixEmptyAndTrim)
        .map(replaceMacroWithEnv)
        .forEach(args::add));

    return args;
  }
}
