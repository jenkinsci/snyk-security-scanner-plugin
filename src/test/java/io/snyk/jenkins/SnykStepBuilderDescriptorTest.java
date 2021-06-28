package io.snyk.jenkins;

import java.util.List;
import java.util.stream.Collectors;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import io.snyk.jenkins.credentials.DefaultSnykApiToken;
import jodd.lagarto.dom.Document;
import jodd.lagarto.dom.LagartoDOMBuilder;
import jodd.lagarto.dom.Node;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
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

    assertThat(model.size(), is(4));
    assertThat(model, hasItems(Severity.LOW.getSeverity(), Severity.MEDIUM.getSeverity(), Severity.HIGH.getSeverity(), Severity.CRITICAL.getSeverity()));
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
  public void doCheckSeverity_shouldReturnWarning_ifProjectNameOverriddenWithAdditionalArguments() {
    Kind severityValidation = instance.doCheckSeverity("high", "--severity-threshold=low").kind;

    assertThat(severityValidation, is(Kind.WARNING));
  }

  @Test
  public void doCheckSnykTokenId_shouldReturnError_ifSnykTokenIsEmpty() throws Exception {
    jenkins.createFreeStyleProject();
    Kind snykTokenIdValidation = instance.doCheckSnykTokenId(jenkins.getInstance().getItems().get(0),null).kind;

    assertThat(snykTokenIdValidation, is(Kind.ERROR));
  }

  @Test
  public void doCheckSnykTokenId_shouldReturnError_ifSnykTokenNotFound() {
    Kind snykTokenIdValidation = instance.doCheckSnykTokenId(null,"any-token").kind;

    assertThat(snykTokenIdValidation, is(Kind.ERROR));
  }

  @Test
  public void doCheckSnykTokenId_shouldReturnOK_ifSnykTokenFound() throws Exception {
    DefaultSnykApiToken snykToken = new DefaultSnykApiToken(CredentialsScope.GLOBAL, "id", "", "snyk-token");
    CredentialsProvider.lookupStores(jenkins.getInstance()).iterator().next().addCredentials(Domain.global(), snykToken);

    Kind snykTokenIdValidation = instance.doCheckSnykTokenId(null, "id").kind;

    assertThat(snykTokenIdValidation, is(Kind.OK));
  }

  @Test
  public void doCheckTargetFile_shouldReturnWarning_ifTargetFileOverriddenWithAdditionalArguments() {
    Kind targetFileValidation = instance.doCheckTargetFile("pom.xml", "--file=pom-extended.xml").kind;

    assertThat(targetFileValidation, is(Kind.WARNING));
  }

  @Test
  public void doCheckOrganisation_shouldReturnWarning_ifTargetFileOverriddenWithAdditionalArguments() {
    Kind organisationValidation = instance.doCheckOrganisation("snyk", "--org=Snyk Ltd").kind;

    assertThat(organisationValidation, is(Kind.WARNING));
  }

  @Test
  public void doCheckProjectName_shouldAggregateWarningMessages() {
    FormValidation projectNameValidation = instance.doCheckProjectName("project-name", "false", "--project-name=new-project");

    assertThat(projectNameValidation.kind, is(Kind.WARNING));

    // Jenkins doesn't provide findings count for 'FormValidation", so we parse html output and check count of <li> elements
    LagartoDOMBuilder domBuilder = new LagartoDOMBuilder();
    Document document = domBuilder.parse(projectNameValidation.renderHtml());
    Node ulNode = document.findChildNodeWithName("ul");

    assertThat(ulNode.getChildNodesCount(), equalTo(2));
  }

  @Test
  public void doCheckProjectName_shouldReturnWarning_ifProjectNameDefinedWithoutMonitorOnBuild() {
    Kind projectNameValidation = instance.doCheckProjectName("project-name", "false", "").kind;

    assertThat(projectNameValidation, is(Kind.WARNING));
  }

  @Test
  public void doCheckProjectName_shouldReturnOK_ifProjectNameDefinedWithMonitorOnBuild() {
    Kind projectNameValidation = instance.doCheckProjectName("project-name", "true", "").kind;

    assertThat(projectNameValidation, is(Kind.OK));
  }
}
