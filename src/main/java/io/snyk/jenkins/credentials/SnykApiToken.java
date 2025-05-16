package io.snyk.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;
import io.snyk.jenkins.SnykContext;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;

/**
 * A Snyk personal API token.
 */
@NameWith(value = SnykApiToken.NameProvider.class, priority = 1)
public interface SnykApiToken extends StandardCredentials {

  @Nonnull
  Secret getToken();

  class NameProvider extends CredentialsNameProvider<SnykApiToken> {

    @Nonnull
    @Override
    public String getName(@Nonnull SnykApiToken credentials) {
      String description = Util.fixEmptyAndTrim(credentials.getDescription());
      return description != null ? description : credentials.getId();
    }
  }

  String SNYK_TOKEN_ENV_KEY = "SNYK_TOKEN";

  static String getToken(SnykContext context, String snykTokenId) {
    return Optional.ofNullable(snykTokenId)
      .map(Util::fixEmptyAndTrim)
      .map(id -> Optional.ofNullable(findCredentialById(id, SnykApiToken.class, context.getRun()))
        .orElseThrow(() -> new RuntimeException("Snyk API token with Credential ID '" + snykTokenId + "' was not found.")))
      .map(SnykApiToken::getToken)
      .map(Secret::getPlainText)
      .map(Util::fixEmptyAndTrim)
      .or(() -> Optional.ofNullable(context.getEnvVars().get(SNYK_TOKEN_ENV_KEY)))
      .orElseThrow(() -> new RuntimeException("Snyk API token not provided. Please assign your credentials to 'snykTokenId' in your build configuration or assign the token to a 'SNYK_TOKEN' build environment variable"));
  }
}
