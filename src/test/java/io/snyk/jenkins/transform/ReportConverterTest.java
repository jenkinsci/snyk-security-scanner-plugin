package io.snyk.jenkins.transform;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class ReportConverterTest {

  @Test
  public void modifyHeadSection_shouldNotChangeHtml_ifHeadTagNotExists() {
    ReportConverter converter = ReportConverter.getInstance();
    String input = "<html><body>text</body></html>";

    String output = converter.modifyHeadSection(input, "/jenkins");

    assertThat(output, equalTo(input));
  }

  @Test
  public void modifyHeadSection_shouldAddExternalCssLink_ifHeadTagExists() {
    ReportConverter converter = ReportConverter.getInstance();
    String input = "<html><head></head><body>text</body></html>";

    String output = converter.modifyHeadSection(input, "/jenkins");

    String expected = "<html><head><link rel=\"stylesheet\" href=\"/jenkins/plugin/snyk-security-scanner/css/snyk_report.css\"></head><body>text</body></html>";
    assertThat(output, equalTo(expected));
  }

  @Test
  public void injectMonitorLink_shouldNotChangeHtml_ifMonitorUriIsEmpty() {
    ReportConverter converter = ReportConverter.getInstance();
    String input = "<html><head></head><body>report text</body></html>";

    String output = converter.injectMonitorLink(input, "");

    assertThat(output, equalTo(input));
  }

  @Test
  public void injectMonitorLink_shouldAddMonitorLink_ifMonitorUriIsNotEmpty() {
    ReportConverter converter = ReportConverter.getInstance();
    String input = "<html><head></head><body>report text</body></html>";

    String output = converter.injectMonitorLink(input, "https://app.snyk.io/my-monitor-report");

    String expected = "<html><head></head><body><center><a target=\"_blank\" href=\"https://app.snyk.io/my-monitor-report\">View On Snyk.io</a></center>report text</body></html>";
    assertThat(output, equalTo(expected));
  }
}
