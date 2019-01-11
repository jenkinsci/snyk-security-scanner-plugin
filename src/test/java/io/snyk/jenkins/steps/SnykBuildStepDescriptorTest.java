package io.snyk.jenkins.steps;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation.Kind;
import io.snyk.jenkins.credentials.DefaultSnykApiToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SnykBuildStepDescriptorTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  private SnykBuildStep.SnykBuildStepDescriptor instance;

  @Before
  public void setUp() {
    instance = new SnykBuildStep.SnykBuildStepDescriptor();
  }

  @Test
  public void doCheckSnykTokenId_shouldReturnError_ifSnykTokenIsEmpty() {
    Kind snykTokenCheck = instance.doCheckSnykTokenId(null).kind;

    assertThat(snykTokenCheck, is(Kind.ERROR));
  }

  @Test
  public void doCheckSnykTokenId_shouldReturnError_ifSnykTokenNotFound() {
    Kind snykTokenCheck = instance.doCheckSnykTokenId("any-token").kind;

    assertThat(snykTokenCheck, is(Kind.ERROR));
  }

  @Test
  public void doCheckSnykTokenId_shouldReturnOK_ifSnykTokenFound() throws Exception {
    DefaultSnykApiToken snykToken = new DefaultSnykApiToken(CredentialsScope.GLOBAL, "id", "", "snyk-token");
    CredentialsProvider.lookupStores(jenkins.getInstance()).iterator().next().addCredentials(Domain.global(), snykToken);

    Kind snykTokenCheck = instance.doCheckSnykTokenId("id").kind;

    assertThat(snykTokenCheck, is(Kind.OK));
  }
}
