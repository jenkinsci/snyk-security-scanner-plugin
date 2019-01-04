package io.snyk.jenkins.credentials;

import javax.annotation.Nonnull;
import java.io.IOException;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;

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
}
