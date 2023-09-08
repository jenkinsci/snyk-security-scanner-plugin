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

import static io.snyk.jenkins.credentials.SnykApiToken.SNYK_TOKEN_ENV_KEY;

public class CommandLine {
  public static ArgumentListBuilder asArgumentList(String executablePath, Command command, SnykConfig config, EnvVars env) {
    Function<String, String> replaceMacroWithEnv = str -> Util.replaceMacro(str, env);
    ArgumentListBuilder args = new ArgumentListBuilder(executablePath, command.commandName());

    if (command == Command.TEST) {
      args.add("--json");
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
