package io.w3security.jenkins.tools.internal;

import io.w3security.jenkins.tools.Platform;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;

import static java.lang.String.format;

public class DownloadService {

  private DownloadService() {
    // squid:S1118
  }

  public static URL getDownloadUrlForW3Security(@Nonnull String version, @Nonnull Platform platform) throws IOException {
    return new URL(format("https://static.w3security.tech/cli/%s/%s", version, platform.w3securityWrapperFileName));
  }

  public static URL getDownloadUrlForW3SecurityToHtml(@Nonnull String version, @Nonnull Platform platform)
      throws IOException {
    return new URL(format("https://static.w3security.tech/w3security-to-html/%s/%s", version,
        platform.w3securityToHtmlWrapperFileName));
  }
}
