package io.snyk.jenkins.command;

import hudson.EnvVars;
import hudson.Util;
import hudson.util.ArgumentListBuilder;
import io.snyk.jenkins.PluginMetadata;
import io.snyk.jenkins.config.SnykConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.snyk.jenkins.credentials.SnykApiToken.SNYK_TOKEN_ENV_KEY;

public class CommandLine {
  public static void validateCommand(String command) {
    if (command == null || command.trim().isEmpty()) {
      return;
    }
    
    String trimmed = command.trim();
    Command cmd = Command.getIfPresent(trimmed);
    
    if (cmd == null) {
      String allowedCommands = Stream.of(Command.values())
        .map(Command::getCommand)
        .collect(Collectors.joining(", "));
      
      throw new IllegalArgumentException(
        "Invalid command: '" + command + "'. " +
        "Allowed commands: " + allowedCommands
      );
    }
  }

  public static ArgumentListBuilder asArgumentList(String executablePath, Command command, SnykConfig config, EnvVars env) {
    return asArgumentList(executablePath, command.getCommand(), config, env);
  }

  public static ArgumentListBuilder asArgumentList(String executablePath, String commandName, SnykConfig config, EnvVars env) {
    Function<String, String> replaceMacroWithEnv = str -> Util.replaceMacro(str, env);
    ArgumentListBuilder args = new ArgumentListBuilder(executablePath);
    
    String[] commandParts = Util.tokenize(commandName);
    for (String part : commandParts) {
      args.add(part);
    }

    if (Command.TEST.getCommand().equals(commandName)) {
      if (!args.toList().contains("--json")) {
        args.add("--json");
      }
    }

    // snyk-to-html requires --reachability for sbom test command
    if (Command.SBOM_TEST.getCommand().equals(commandName)) {
      if (!args.toList().contains("--reachability")) {
        args.add("--reachability");
      }
      if (!args.toList().contains("--json")) {
        args.add("--json");
      }
    }

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

  public static Map<String, String> asEnvVars(String snykToken, EnvVars envVars) {
    HashMap<String, String> result = new HashMap<>(envVars);
    Optional.ofNullable(snykToken).ifPresent(token -> result.put(SNYK_TOKEN_ENV_KEY, token));
    result.put("SNYK_INTEGRATION_NAME", PluginMetadata.getIntegrationName());
    result.put("SNYK_INTEGRATION_VERSION", PluginMetadata.getIntegrationVersion());
    result.put("SNYK_INTEGRATION_ENVIRONMENT", PluginMetadata.getIntegrationEnvironment());
    result.put("SNYK_INTEGRATION_ENVIRONMENT_VERSION", PluginMetadata.getIntegrationEnvironmentVersion());
    return result;
  }

  public static Map<String, String> asEnvVars(EnvVars envVars) {
    return asEnvVars(null, envVars);
  }
}
