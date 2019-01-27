package io.snyk.jenkins;

import javax.annotation.Nonnull;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class SnykReportBuildAction implements RunAction2 {

  private transient Run<?, ?> run;

  public SnykReportBuildAction(@Nonnull Run<?, ?> run) {
    this.run = run;
  }

  @Override
  public void onAttached(Run<?, ?> run) {
    this.run = run;
  }

  @Override
  public void onLoad(Run<?, ?> run) {
    this.run = run;
  }

  @SuppressWarnings("unused")
  public Run<?, ?> getRun() {
    return run;
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
    return "snykReport";
  }
}
