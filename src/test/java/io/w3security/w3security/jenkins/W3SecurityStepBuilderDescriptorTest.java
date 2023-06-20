package io.w3security.jenkins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import io.w3security.jenkins.credentials.DefaultW3SecurityApiToken;
import jodd.lagarto.dom.Document;
import jodd.lagarto.dom.LagartoDOMBuilder;
import jodd.lagarto.dom.Node;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;

public class W3SecurityStepBuilderDescriptorTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  private W3SecurityStepBuilder.W3SecurityStepBuilderDescriptor instance;

  @Before
  public void setUp() {
    instance = new W3SecurityStepBuilder.W3SecurityStepBuilderDescriptor();
  }

  @Test
  public void doFillSeverityItems_shouldReturnAllValuesFromSeverityEnum() {
    List<String> model = instance.doFillSeverityItems().stream()
        .map(e -> e.value)
        .collect(Collectors.toList());

    assertThat(model.size(), is(4));
    assertThat(model, hasItems(Severity.LOW.getSeverity(), Severity.MEDIUM.getSeverity(), Severity.HIGH.getSeverity(),
        Severity.CRITICAL.getSeverity()));
  }

  @Test
  public void doFillW3SecurityTokenIdItems_shouldAddCurrentValue_ifPresent() {
    ListBoxModel model = instance.doFillW3SecurityTokenIdItems(null, "current-value");

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
  public void doCheckW3SecurityTokenId_shouldReturnWarning_ifW3SecurityTokenIsEmpty() throws Exception {
    jenkins.createFreeStyleProject();
    Kind w3securityTokenIdValidation = instance.doCheckW3SecurityTokenId(jenkins.getInstance().getItems().get(0), null).kind;

    assertThat(w3securityTokenIdValidation, is(Kind.WARNING));
  }

  @Test
  public void doCheckW3SecurityTokenId_shouldReturnError_ifW3SecurityTokenNotFound() {
    Kind w3securityTokenIdValidation = instance.doCheckW3SecurityTokenId(null, "any-token").kind;

    assertThat(w3securityTokenIdValidation, is(Kind.ERROR));
  }

  @Test
  public void doCheckW3SecurityTokenId_shouldReturnOK_ifW3SecurityTokenFound() throws Exception {
    DefaultW3SecurityApiToken w3securityToken = new DefaultW3SecurityApiToken(CredentialsScope.GLOBAL, "id", "",
        "w3security-token");
    CredentialsProvider.lookupStores(jenkins.getInstance()).iterator().next().addCredentials(Domain.global(),
        w3securityToken);

    Kind w3securityTokenIdValidation = instance.doCheckW3SecurityTokenId(null, "id").kind;

    assertThat(w3securityTokenIdValidation, is(Kind.OK));
  }

  @Test
  public void doCheckTargetFile_shouldReturnWarning_ifTargetFileOverriddenWithAdditionalArguments() {
    Kind targetFileValidation = instance.doCheckTargetFile("pom.xml", "--file=pom-extended.xml").kind;

    assertThat(targetFileValidation, is(Kind.WARNING));
  }

  @Test
  public void doCheckOrganisation_shouldReturnWarning_ifTargetFileOverriddenWithAdditionalArguments() {
    Kind organisationValidation = instance.doCheckOrganisation("w3security", "--org=W3Security Ltd").kind;

    assertThat(organisationValidation, is(Kind.WARNING));
  }

  @Test
  public void doCheckProjectName_shouldAggregateWarningMessages() {
    FormValidation projectNameValidation = instance.doCheckProjectName("project-name", "false",
        "--project-name=new-project");

    assertThat(projectNameValidation.kind, is(Kind.WARNING));

    // Jenkins doesn't provide findings count for 'FormValidation", so we parse html
    // output and check count of <li> elements
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
