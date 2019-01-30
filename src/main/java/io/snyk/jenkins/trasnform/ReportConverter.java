package io.snyk.jenkins.trasnform;

import javax.annotation.Nonnull;
import java.util.Objects;

import jodd.jerry.Jerry;

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

  public String modifyHeadSection(String htmlWithInlineCSS) {
    Jerry document = parser.parse(htmlWithInlineCSS);

    // remove inline css (it's disabled by jenkins)
    document.$("head").$("style").remove();
    // append external css
    document.$("head").append("<link rel=\"stylesheet\" href=\"/plugin/snyk-security-scanner/css/snyk_report.css\">");
    return document.html();
  }
}
