package io.snyk.jenkins.tools.internal;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class DownloadService {

  public static String loadJSON(String source) throws IOException {
    final URL sourceUrl = new URL(source);
    return IOUtils.toString(sourceUrl, "UTF-8");
  }
}
