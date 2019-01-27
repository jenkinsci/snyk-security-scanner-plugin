package io.snyk.jenkins;

import javax.annotation.Nonnull;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class SnykReportBuildAction implements RunAction2 {

  private Run<?, ?> run;
  private String resultUrl;


  public SnykReportBuildAction(@Nonnull Run<?, ?> run, String artifactName) {
    this.run = run;
    this.resultUrl = "../artifact/" + artifactName;
  }

  @Override
  public void onAttached(Run<?, ?> r) {
  }

  @Override
  public void onLoad(Run<?, ?> r) {
  }

  @Override
  public String getIconFileName() {
    return "/plugin/snyk-security-scanner/img/icon.png";
  }

  @Override
  public String getDisplayName() {
    return "Snyk Security Report";
  }

  @Override
  public String getUrlName() {
    return "snyk-results";
  }

  public Run<?, ?> getRun() {
    return run;
  }

  public String getResultUrl() {
    return resultUrl;
  }
}
