package io.snyk.jenkins;

import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.util.VirtualFile;
import org.kohsuke.stapler.Stapler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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
  public String getArtifactContent() {
    String filename = Stapler.getCurrentRequest2().getParameter("artifact");
    VirtualFile artifact = getArtifact(filename);
    return readFile(artifact);
  }

  private String readFile(VirtualFile file) {
    try (
      InputStream is = file.open();
      InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
      BufferedReader br = new BufferedReader(isr)
    ) {
      return br.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private VirtualFile getArtifact(String filename) {
    return run.getArtifacts().stream()
      .filter(a -> a.getFileName().equals(filename))
      .map(a -> run.getArtifactManager().root().child(a.relativePath))
      .findFirst()
      .orElseThrow(() -> new RuntimeException("Could not find artifact."));
  }
}
