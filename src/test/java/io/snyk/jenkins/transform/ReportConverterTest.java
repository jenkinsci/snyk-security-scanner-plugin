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

}
