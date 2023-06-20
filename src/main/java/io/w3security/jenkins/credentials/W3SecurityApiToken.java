package io.w3security.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;
import io.w3security.jenkins.W3SecurityContext;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;

/**
 * A W3Security personal API token.
 */
@NameWith(value = W3SecurityApiToken.NameProvider.class, priority = 1)
public interface W3SecurityApiToken extends StandardCredentials {

  @Nonnull
  Secret getToken();

  class NameProvider extends CredentialsNameProvider<W3SecurityApiToken> {

    @Nonnull
    @Override
    public String getName(@Nonnull W3SecurityApiToken credentials) {
      String description = Util.fixEmptyAndTrim(credentials.getDescription());
      return description != null ? description : credentials.getId();
    }
  }

  String W3SECURITY_TOKEN_ENV_KEY = "W3SECURITY_TOKEN";

  static String getToken(W3SecurityContext context, String w3securityTokenId) {
    return Optional.ofNullable(w3securityTokenId)
        .map(Util::fixEmptyAndTrim)
        .map(id -> Optional.ofNullable(findCredentialById(id, W3SecurityApiToken.class, context.getRun()))
            .orElseThrow(() -> new RuntimeException(
                "W3Security API token with Credential ID '" + w3securityTokenId + "' was not found.")))
        .map(W3SecurityApiToken::getToken)
        .map(Secret::getPlainText)
        .map(Util::fixEmptyAndTrim)
        .map(Optional::of)
        .orElseGet(() -> Optional.ofNullable(context.getEnvVars().get(W3SECURITY_TOKEN_ENV_KEY)))
        .orElseThrow(() -> new RuntimeException(
            "W3Security API token not provided. Please assign your credentials to 'w3securityTokenId' in your build configuration or assign the token to a 'W3SECURITY_TOKEN' build environment variable"));
  }
}
