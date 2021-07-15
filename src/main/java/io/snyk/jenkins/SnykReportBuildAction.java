package io.snyk.jenkins;

import hudson.model.Run;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.Stapler;

public class SnykReportBuildAction implements RunAction2 {

  private transient Run<?, ?> run;

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

  @SuppressWarnings("unused")
  public String getArtifactHref() {
    return "../artifact/" + Stapler.getCurrentRequest().getParameter("artifact");
  }
}
