package io.snyk.jenkins.transform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

import jodd.jerry.Jerry;

import static java.lang.String.format;

public class ReportConverter {

  private final Jerry.JerryParser parser;

  private static class Helper {
    private static final ReportConverter INSTANCE = new ReportConverter();
  }

  private ReportConverter() {
    parser = Objects.requireNonNull(Jerry.jerry());
  }

  @Nonnull
  public static ReportConverter getInstance() {
    return Helper.INSTANCE;
  }

  public String modifyHeadSection(@Nonnull String htmlWithInlineCSS) {
    Jerry document = parser.parse(htmlWithInlineCSS);

    // remove inline css (it's disabled by jenkins)
    document.$("head").$("style").remove();
    // append external css
    document.$("head").append("<link rel=\"stylesheet\" href=\"/plugin/snyk-security-scanner/css/snyk_report.css\">");
    return document.html();
  }

  public String injectMonitorLink(@Nonnull String html, @Nullable String monitorUri) {
    if (monitorUri == null || monitorUri.isEmpty()) {
      return html;
    }

    Jerry document = parser.parse(html);
    String monitorHtmlSnippet = format("<center><a target=\"_blank\" href=\"%s\">View On Snyk.io</a></center>", monitorUri);
    // prepend monitor link as first element after body
    document.$("body").prepend(monitorHtmlSnippet);
    return document.html();
  }
}
