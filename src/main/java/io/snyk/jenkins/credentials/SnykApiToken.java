package io.snyk.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;
import io.snyk.jenkins.SnykContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;

import static com.cloudbees.plugins.credentials.CredentialsProvider.findCredentialById;

/**
 * A Snyk personal API token.
 */
@NameWith(value = SnykApiToken.NameProvider.class, priority = 1)
public interface SnykApiToken extends StandardCredentials {

  @Nonnull
  Secret getToken() throws IOException, InterruptedException;

  class NameProvider extends CredentialsNameProvider<SnykApiToken> {

    @Nonnull
    @Override
    public String getName(@Nonnull SnykApiToken credentials) {
      String description = Util.fixEmptyAndTrim(credentials.getDescription());
      return description != null ? description : credentials.getId();
    }
  }

  static String getToken(SnykContext context, String snykTokenId) throws IOException, InterruptedException {
    return Optional.ofNullable(findCredentialById(snykTokenId, SnykApiToken.class, context.getRun()))
      .orElseThrow(() -> new IOException("Snyk API token with ID '" + snykTokenId + "' was not found. Please configure the build properly and retry."))
      .getToken()
      .getPlainText();
  }
}
