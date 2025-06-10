package io.snyk.jenkins.tools.internal;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.snyk.jenkins.PluginMetadata;
import io.snyk.jenkins.tools.Platform;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static java.lang.String.format;

public class DownloadService {
  private static final String SNYK_DOWNLOAD_PRIMARY = "https://downloads.snyk.io/%s/%s/%s";
  private static final String SNYK_DOWNLOAD_SECONDARY = "https://static.snyk.io/%s/%s/%s";
  public static final List<String> SNYK_CLI_DOWNLOAD_URLS = List.of(SNYK_DOWNLOAD_PRIMARY, SNYK_DOWNLOAD_SECONDARY);

  private DownloadService() {
    // squid:S1118
  }

  public static URL constructDownloadUrlForSnyk(@NonNull String urlTemplate, @NonNull String product, @NonNull String version, @NonNull Platform platform) throws IOException {
    URL urlNoUtm;

    if (product.equals("cli")) {
      urlNoUtm = new URL(format(urlTemplate, product, version, platform.snykWrapperFileName));
    } else { // snyk-to-html
      urlNoUtm = new URL(format(urlTemplate, product, version, platform.snykToHtmlWrapperFileName));
    }
    return new URL(urlNoUtm + "?utm_source=" + PluginMetadata.getIntegrationName());
  }
}
