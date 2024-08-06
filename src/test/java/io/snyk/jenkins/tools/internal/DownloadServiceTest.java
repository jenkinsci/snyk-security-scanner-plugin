package io.snyk.jenkins.tools.internal;

import io.snyk.jenkins.PluginMetadata;
import org.junit.Test;

import io.snyk.jenkins.tools.Platform;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;


public class DownloadServiceTest {

  @Test
  public void constructDownloadUrlForSnyk_shouldReturnExpectedUrlForCli() throws IOException {
    String urlTemplate = "https://downloads.snyk.io/%s/%s/%s";
    String product = "cli";
    String version = "stable";
    String queryParam = "?utm_source=" + PluginMetadata.getIntegrationName();
    Platform platform = Platform.MAC_OS;

    URL expectedUrl = new URL("https://downloads.snyk.io/" + product + "/" + version + "/" + "snyk-macos" + queryParam);
    URL actualUrl = DownloadService.constructDownloadUrlForSnyk(urlTemplate, product, version, platform);

    assertThat(actualUrl, notNullValue());
    assertThat(actualUrl, equalTo(expectedUrl));
  }

  @Test
  public void constructDownloadUrlForSnyk_shouldReturnExpectedUrlForSnykToHtml() throws IOException {
    String urlTemplate = "https://downloads.snyk.io/%s/%s/%s";
    String product = "snyk-to-html";
    String version = "stable";
    String queryParam = "?utm_source=" + PluginMetadata.getIntegrationName();
    Platform platform = Platform.MAC_OS;

    URL expectedUrl = new URL("https://downloads.snyk.io/" + product + "/" + version + "/" + "snyk-to-html-macos" + queryParam);
    URL actualUrl = DownloadService.constructDownloadUrlForSnyk(urlTemplate, product, version, platform);

    assertThat(actualUrl, notNullValue());
    assertThat(actualUrl, equalTo(expectedUrl));
  }
}
