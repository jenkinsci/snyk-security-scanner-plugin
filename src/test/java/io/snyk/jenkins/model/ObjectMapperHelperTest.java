package io.snyk.jenkins.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ObjectMapperHelperTest {

  private static File TEST_FIXTURES_DIRECTORY;

  @BeforeClass
  public static void setupAll() throws Exception {
    URL testDirectoryUrl = ObjectMapperHelper.class.getClassLoader().getResource("./fixtures");
    TEST_FIXTURES_DIRECTORY = new File(requireNonNull(testDirectoryUrl).toURI());
  }

  @Test
  public void unmarshallMonitorResult_withURI() throws IOException {
    Path monitorReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "monitor", "snyk_monitor_with_uri.json");
    String monitorReport = new String(Files.readAllBytes(monitorReportPath));

    SnykMonitorResult snykMonitorResult = ObjectMapperHelper.unmarshallMonitorResult(monitorReport);

    assertThat(snykMonitorResult.uri, equalTo("http://localhost:8000/org/dummy-user/project/long-uuid-here/history/long-uuid-here-again"));
  }

  @Test
  public void unmarshallMonitorResult_withoutURI() throws IOException {
    Path monitorReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "monitor", "snyk_monitor_without_uri.json");
    String monitorReport = new String(Files.readAllBytes(monitorReportPath));

    SnykMonitorResult snykMonitorResult = ObjectMapperHelper.unmarshallMonitorResult(monitorReport);

    assertThat(snykMonitorResult.uri, nullValue());
  }

  @Test
  public void unmarshallTestResult_singleModule_withVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "single-module-project", "snyk_report_with_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(snykTestResult.ok, equalTo(false));
    assertThat(snykTestResult.dependencyCount, equalTo(10));
    assertThat(snykTestResult.uniqueCount, equalTo(3));
  }

  @Test
  public void unmarshallTestResult_singleModule_withoutVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "single-module-project", "snyk_report_without_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(snykTestResult.ok, equalTo(true));
    assertThat(snykTestResult.dependencyCount, equalTo(33));
    assertThat(snykTestResult.uniqueCount, equalTo(0));
  }

  @Test
  public void unmarshallTestResult_multiModule_withVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "multi-module-project", "snyk_report_with_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(snykTestResult.ok, equalTo(false));
    assertThat(snykTestResult.dependencyCount, equalTo(6));
    assertThat(snykTestResult.uniqueCount, equalTo(1));
  }

  @Test
  public void unmarshallTestResult_multiModule_withoutVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "multi-module-project", "snyk_report_without_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    SnykTestResult snykTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(snykTestResult.ok, equalTo(true));
    assertThat(snykTestResult.dependencyCount, equalTo(20));
    assertThat(snykTestResult.uniqueCount, equalTo(0));
  }
}
