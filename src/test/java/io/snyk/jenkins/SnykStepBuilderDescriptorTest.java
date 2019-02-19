package io.snyk.jenkins;

import java.util.List;
import java.util.stream.Collectors;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.credentials.DefaultSnykApiToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;

public class SnykStepBuilderDescriptorTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  private SnykStepBuilder.SnykStepBuilderDescriptor instance;

  @Before
  public void setUp() {
    instance = new SnykStepBuilder.SnykStepBuilderDescriptor();
  }

  @Test
  public void doFillSeverityItems_shouldReturnAllValuesFromSeverityEnum() {
    List<String> model = instance.doFillSeverityItems().stream()
                                 .map(e -> e.value)
                                 .collect(Collectors.toList());

    assertThat(model.size(), is(3));
    assertThat(model, hasItems(Severity.LOW.getSeverity(), Severity.MEDIUM.getSeverity(), Severity.HIGH.getSeverity()));
  }

  @Test
  public void doFillSnykTokenIdItems_shouldAddCurrentValue_ifPresent() {
    ListBoxModel model = instance.doFillSnykTokenIdItems(null, "current-value");

    assertThat(model.size(), is(2));

    ListBoxModel.Option firstItem = model.get(0);
    assertThat(firstItem.value, is(""));

    ListBoxModel.Option secondItem = model.get(1);
    assertThat(secondItem.value, is("current-value"));
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

  @Test
  public void doCheckProjectName_shouldReturnWarning_ifProjectNameDefinedWithoutMonitorOnBuild() {
    Kind doCheckProjectNameCheck = instance.doCheckProjectName("project-name", "false").kind;

    assertThat(doCheckProjectNameCheck, is(Kind.WARNING));
  }

  @Test
  public void doCheckProjectName_shouldReturnOK_ifProjectNameDefinedWithMonitorOnBuild() {
    Kind doCheckProjectNameCheck = instance.doCheckProjectName("project-name", "true").kind;

    assertThat(doCheckProjectNameCheck, is(Kind.OK));
  }
}
