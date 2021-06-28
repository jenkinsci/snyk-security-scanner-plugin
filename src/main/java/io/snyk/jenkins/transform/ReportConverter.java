package io.snyk.jenkins.transform;

import jenkins.model.Jenkins;
import jodd.jerry.Jerry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

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

  /**
   * Jenkins has Content-Security-Policy headers which reject inline styles.
   * So we must replace it with a URL to a CSS file under the same host.
   * - https://wiki.jenkins.io/display/JENKINS/Hyperlinks+in+HTML
   * - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/style-src
   */
  public String modifyHeadSection(@Nonnull String htmlWithInlineCSS, String contextPath) {
    String cssHref = contextPath + "/plugin/snyk-security-scanner/css/snyk_report.css";
    Jerry document = parser.parse(htmlWithInlineCSS);
    document.$("head").$("style").remove();
    document.$("head").append("<link rel=\"stylesheet\" href=\"" + cssHref + "\">");
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
