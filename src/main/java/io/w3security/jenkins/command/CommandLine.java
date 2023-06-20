package io.w3security.jenkins.command;

import hudson.EnvVars;
import hudson.Util;
import hudson.util.ArgumentListBuilder;
import io.w3security.jenkins.PluginMetadata;
import io.w3security.jenkins.config.W3SecurityConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static io.w3security.jenkins.credentials.W3SecurityApiToken.W3SECURITY_TOKEN_ENV_KEY;

public class CommandLine {
  public static ArgumentListBuilder asArgumentList(String executablePath, Command command, W3SecurityConfig config,
      EnvVars env) {
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

  public static Map<String, String> asEnvVars(String w3securityToken, EnvVars envVars) {
    HashMap<String, String> result = new HashMap<>(envVars);
    Optional.ofNullable(w3securityToken).ifPresent(token -> result.put(W3SECURITY_TOKEN_ENV_KEY, token));
    result.put("W3SECURITY_INTEGRATION_NAME", PluginMetadata.getIntegrationName());
    result.put("W3SECURITY_INTEGRATION_VERSION", PluginMetadata.getIntegrationVersion());
    return result;
  }

  public static Map<String, String> asEnvVars(EnvVars envVars) {
    return asEnvVars(null, envVars);
  }
}
