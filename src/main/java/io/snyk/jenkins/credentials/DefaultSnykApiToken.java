package io.snyk.jenkins.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import static hudson.util.Secret.fromString;

/**
 * Default implementation of {@link SnykApiToken} for use by Jenkins {@link com.cloudbees.plugins.credentials.CredentialsProvider}
 * instances that store {@link Secret} locally.
 */
public class DefaultSnykApiToken extends BaseStandardCredentials implements SnykApiToken {

  @NonNull
  private final Secret token;

  @DataBoundConstructor
  public DefaultSnykApiToken(CredentialsScope scope, String id, String description, @NonNull String token) {
    super(scope, id, description);
    this.token = fromString(token);
  }

  @NonNull
  @Override
  public Secret getToken() {
    return token;
  }

  @Extension
  public static class DefaultSnykApiTokenDescriptor extends BaseStandardCredentialsDescriptor {

    @NonNull
    @Override
    public String getDisplayName() {
      return "Snyk API token";
    }
  }
}
