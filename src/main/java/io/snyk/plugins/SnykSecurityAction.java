package io.snyk.plugins;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;

import javax.annotation.Nonnull;

public class SnykSecurityAction implements Action {

    private String resultsUrl;
    private Run<?,?> build;

    public SnykSecurityAction(AbstractBuild<?,?> build, String artifactName) {
            this.build = build;
        this.resultsUrl = "../artifact/" + artifactName;
    }

    public SnykSecurityAction(@Nonnull Run<?, ?> build, String artifactName) {
        this.build = build;
        this.resultsUrl = "../artifact/" + artifactName;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/snyk-security-scanner/images/snyk_icon.png";
    }

    @Override
    public String getDisplayName() {
        return "Snyk Security Report";
    }

    @Override
    public String getUrlName() {
        return "snyk-reports";
    }

    public Run<?,?> getBuild() {
        return this.build;
    }

    public String getResultsUrl() {
        return this.resultsUrl;
    }
}
