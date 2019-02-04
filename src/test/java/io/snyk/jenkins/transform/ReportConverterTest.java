package io.snyk.jenkins.transform;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;

public class ReportConverterTest {

  private ReportConverter converter = ReportConverter.getInstance();

  @Test
  public void modifyHeadSection_shouldNotChangeHtml_ifHeadTagNotExists() {
    String inputHtml = "<html><body>text</body></html>";

    String outputHtml = converter.modifyHeadSection(inputHtml);

    Assert.assertThat(outputHtml, equalTo(inputHtml));
  }

  @Test
  public void modifyHeadSection_shouldAddExternalCssLink_ifHeadTagExists() {
    String expectedHtml = "<html><head><link rel=\"stylesheet\" href=\"/plugin/snyk-security-scanner/css/snyk_report.css\"></head><body>text</body></html>";
    String inputHtml = "<html><head></head><body>text</body></html>";

    String outputHtml = converter.modifyHeadSection(inputHtml);

    Assert.assertThat(outputHtml, equalTo(expectedHtml));
  }
}
