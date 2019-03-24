package io.snyk.jenkins.tools.internal;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;

import io.snyk.jenkins.tools.Platform;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;

import static java.lang.String.format;

public class DownloadService {

  private static final String SNYK_RELEASES_LATEST = "https://api.github.com/repos/snyk/snyk/releases/latest";
  private static final String SNYK_RELEASES_TAGS = "https://api.github.com/repos/snyk/snyk/releases/tags/v%s";
  private static final String SNYK_HTML_RELEASES_LATEST = "https://api.github.com/repos/snyk/snyk-to-html/releases/latest";

  private DownloadService() {
    // squid:S1118
  }

  public static URL getDownloadUrlForSnyk(@Nonnull String version, @Nonnull Platform platform) throws IOException {
    String jsonString;
    String tagName;

    // latest version needed different url
    if ("latest".equals(version)) {
      jsonString = loadJSON(SNYK_RELEASES_LATEST);
    } else {
      jsonString = loadJSON(format(SNYK_RELEASES_TAGS, version));
    }
    JSONObject release = JSONObject.fromObject(jsonString);
    tagName = (String) release.get("tag_name");
    return new URL(format("https://github.com/snyk/snyk/releases/download/%s/%s", tagName, platform.snykWrapperFileName));
  }

  public static URL getDownloadUrlForSnykToHtml(@Nonnull Platform platform) throws IOException {
    String jsonString = loadJSON(SNYK_HTML_RELEASES_LATEST);
    JSONObject release = JSONObject.fromObject(jsonString);
    String tagName = (String) release.get("tag_name");
    return new URL(format("https://github.com/snyk/snyk-to-html/releases/download/%s/%s", tagName, platform.snykToHtmlWrapperFileName));
  }

  private static String loadJSON(String source) throws IOException {
    final URL sourceUrl = new URL(source);
    return IOUtils.toString(sourceUrl, "UTF-8");
  }
}
