package io.snyk.jenkins.tools.internal;

import io.snyk.jenkins.tools.Platform;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;

import static java.lang.String.format;

public class DownloadService {

  private DownloadService() {
    // squid:S1118
  }

  public static URL getDownloadUrlForSnyk(@Nonnull String version, @Nonnull Platform platform) throws IOException {
    return new URL(format("https://static.snyk.io/cli/%s/%s", version, platform.snykWrapperFileName));
  }

  public static URL getDownloadUrlForSnykToHtml(@Nonnull String version, @Nonnull Platform platform) throws IOException {
    return new URL(format("https://static.snyk.io/snyk-to-html/%s/%s", version, platform.snykToHtmlWrapperFileName));
  }
}
