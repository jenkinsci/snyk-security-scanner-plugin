package io.w3security.jenkins.model;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class ObjectMapperHelperTest {

  private static File TEST_FIXTURES_DIRECTORY;

  @BeforeClass
  public static void setupAll() throws Exception {
    URL testDirectoryUrl = ObjectMapperHelper.class.getClassLoader().getResource("./fixtures");
    TEST_FIXTURES_DIRECTORY = new File(requireNonNull(testDirectoryUrl).toURI());
  }

  @Test
  public void unmarshallTestResult_singleModule_withVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "single-module-project",
        "w3security_report_with_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    W3SecurityTestResult w3securityTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(w3securityTestResult.ok, equalTo(false));
    assertThat(w3securityTestResult.dependencyCount, equalTo(10));
    assertThat(w3securityTestResult.uniqueCount, equalTo(3));
  }

  @Test
  public void unmarshallTestResult_singleModule_withoutVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "single-module-project",
        "w3security_report_without_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    W3SecurityTestResult w3securityTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(w3securityTestResult.ok, equalTo(true));
    assertThat(w3securityTestResult.dependencyCount, equalTo(33));
    assertThat(w3securityTestResult.uniqueCount, equalTo(0));
  }

  @Test
  public void unmarshallTestResult_multiModule_withVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "multi-module-project",
        "w3security_report_with_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    W3SecurityTestResult w3securityTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(w3securityTestResult.ok, equalTo(false));
    assertThat(w3securityTestResult.dependencyCount, equalTo(6));
    assertThat(w3securityTestResult.uniqueCount, equalTo(1));
  }

  @Test
  public void unmarshallTestResult_multiModule_withoutVulns() throws IOException {
    Path testReportPath = Paths.get(TEST_FIXTURES_DIRECTORY.getAbsolutePath(), "test", "multi-module-project",
        "w3security_report_without_vulns.json");
    String testReport = new String(Files.readAllBytes(testReportPath));

    W3SecurityTestResult w3securityTestResult = ObjectMapperHelper.unmarshallTestResult(testReport);

    assertThat(w3securityTestResult.ok, equalTo(true));
    assertThat(w3securityTestResult.dependencyCount, equalTo(20));
    assertThat(w3securityTestResult.uniqueCount, equalTo(0));
  }
}
