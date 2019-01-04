package io.snyk.jenkins.credentials;

import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import static hudson.util.Secret.fromString;

/**
 * Default implementation of {@link SnykApiToken} for use by Jenkins {@link com.cloudbees.plugins.credentials.CredentialsProvider}
 * instances that store {@link Secret} locally.
 */
public class DefaultSnykApiToken extends BaseStandardCredentials implements SnykApiToken {

  @Nonnull
  private final Secret token;

  @DataBoundConstructor
  public DefaultSnykApiToken(CredentialsScope scope, String id, String description, @Nonnull String token) {
    super(scope, id, description);
    this.token = fromString(token);
  }

  @Nonnull
  @Override
  public Secret getToken() {
    return token;
  }

  @Extension
  public static class DefaultSnykApiTokenDescriptor extends BaseStandardCredentialsDescriptor {

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Snyk API token";
    }
  }
}
