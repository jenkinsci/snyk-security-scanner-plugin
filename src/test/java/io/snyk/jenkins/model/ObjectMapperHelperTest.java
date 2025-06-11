package io.snyk.jenkins.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

class ObjectMapperHelperTest {

  private static File TEST_FIXTURES_DIRECTORY;

  @BeforeAll
  static void setupAll() throws Exception {
    URL testDirectoryUrl = ObjectMapperHelper.class.getClassLoader().getResource("./fixtures");
    TEST_FIXTURES_DIRECTORY = new File(requireNonNull(testDirectoryUrl).toURI());
  }

  @Test
  void unmarshallTestResult_singleModule_withVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "single-module-project", "snyk_report_with_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(snykTestResult.ok, equalTo(false));
    assertThat(snykTestResult.dependencyCount, equalTo(10));
    assertThat(snykTestResult.uniqueCount, equalTo(3));
  }

  @Test
  void unmarshallTestResult_singleModule_withoutVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "single-module-project", "snyk_report_without_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(snykTestResult.ok, equalTo(true));
    assertThat(snykTestResult.dependencyCount, equalTo(33));
    assertThat(snykTestResult.uniqueCount, equalTo(0));
  }

  @Test
  void unmarshallTestResult_multiModule_withVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "multi-module-project", "snyk_report_with_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(snykTestResult.ok, equalTo(false));
    assertThat(snykTestResult.dependencyCount, equalTo(6));
    assertThat(snykTestResult.uniqueCount, equalTo(1));
  }

  @Test
  void unmarshallTestResult_multiModule_withoutVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "multi-module-project", "snyk_report_without_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(snykTestResult.ok, equalTo(true));
    assertThat(snykTestResult.dependencyCount, equalTo(20));
    assertThat(snykTestResult.uniqueCount, equalTo(0));
  }
}
