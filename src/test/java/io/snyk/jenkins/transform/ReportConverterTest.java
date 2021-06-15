package io.snyk.jenkins.transform;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class ReportConverterTest {

  private ReportConverter converter = ReportConverter.getInstance();

  @Test
  public void modifyHeadSection_shouldNotChangeHtml_ifHeadTagNotExists() {
    String inputHtml = "<html><body>text</body></html>";
    String outputHtml = converter.modifyHeadSection(inputHtml, "/jenkins");

    assertThat(outputHtml, equalTo(inputHtml));
  }

  @Test
  public void modifyHeadSection_shouldAddExternalCssLink_ifHeadTagExists() {
    String expectedHtml = "<html><head><link rel=\"stylesheet\" href=\"/jenkins/plugin/snyk-security-scanner/css/snyk_report.css\"></head><body>text</body></html>";
    String inputHtml = "<html><head></head><body>text</body></html>";

    String outputHtml = converter.modifyHeadSection(inputHtml, "/jenkins");

    assertThat(outputHtml, equalTo(expectedHtml));
  }

  @Test
  public void injectMonitorLink_shouldNotChangeHtml_ifMonitorUriIsEmpty() {
    String inputHtml = "<html><head></head><body>report text</body></html>";

    String outputHtml = converter.injectMonitorLink(inputHtml, "");

    assertThat(outputHtml, equalTo(inputHtml));
  }

  @Test
  public void injectMonitorLink_shouldAddMonitorLink_ifMonitorUriIsNotEmpty() {
    String expectedHtml = "<html><head></head><body><center><a target=\"_blank\" href=\"https://app.snyk.io/my-monitor-report\">View On Snyk.io</a></center>report text</body></html>";
    String inputHtml = "<html><head></head><body>report text</body></html>";

    String outputHtml = converter.injectMonitorLink(inputHtml, "https://app.snyk.io/my-monitor-report");

    assertThat(outputHtml, equalTo(expectedHtml));
  }
}
