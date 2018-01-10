/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.snyk.plugins;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class SnykSecurityStep extends AbstractStepImpl {
    private String token;
    private String targetFile;
    private String organization;
    private boolean monitor;
    private String tokenCredentialId;
    private boolean failOnBuild;
    private String envVars;
    private String dockerImage;
    private String projectName;
    private String httpProxy;
    private String httpsProxy;

    @DataBoundConstructor
    public SnykSecurityStep() {}
    public String getToken() {
        return token;
    }

    @DataBoundSetter
    public void setToken(String token) {
        this.token = token;
    }

    @DataBoundSetter
    public void setMonitor(boolean monitor) {
        this.monitor = monitor;
    }

    public boolean getMonitor() {
        return this.monitor;
    }

    @DataBoundSetter
    public void setTargetFile(String targetFile) {
        this.targetFile = targetFile;
    }

    public String getTargetFile() {
        return this.targetFile;
    }

    @DataBoundSetter
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganization() {
        return this.organization;
    }

    @DataBoundSetter
    public void setFailOnBuild(boolean failOnBuild) {
        this.failOnBuild = failOnBuild;
    }

    public boolean getFailOnBuild() {
        return this.failOnBuild;
    }

    public String getTokenCredentialId() {
        return tokenCredentialId;
    }

    @DataBoundSetter
    public void setTokenCredentialId(String tokenCredentialId) {
        this.tokenCredentialId = Util.fixEmpty(tokenCredentialId);
    }

    public String getEnvVars() {
        return envVars;
    }

    @DataBoundSetter
    public void setEnvVars(String envVars) {
        this.envVars = envVars;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    @DataBoundSetter
    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public String getProjectName() {
        return projectName;
    }

    @DataBoundSetter
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getHttpProxy() {
        return httpProxy;
    }

    @DataBoundSetter
    public void setHttpProxy(String httpProxy) {
        this.httpProxy = httpProxy;
    }

    public String getHttpsProxy() {
        return httpsProxy;
    }

    @DataBoundSetter
    public void setHttpsProxy(String httpsProxy) {
        this.httpsProxy = httpsProxy;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() { super(SnykSecurityStepExecution.class); }

        @Override
        public String getFunctionName() {
            return "snykSecurity";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Snyk Security report";
        }
    }

    public static class SnykSecurityStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @Inject
        transient SnykSecurityStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient Launcher launcher;

        @Override
        protected Void run() throws Exception {
            listener.getLogger().println("Running Snyk security step.");
            String realToken = getTokenToUse();
            if (realToken == null || realToken.isEmpty()) {
                listener.getLogger().println("Snyk token is required");
                throw new Exception("Snyk token is required");
            }

            SnykSecurityBuilder builder = new SnykSecurityBuilder(
                    String.valueOf(step.failOnBuild), step.monitor,
                    step.targetFile, step.organization, step.envVars,
                    step.dockerImage, step.projectName, step.httpProxy,
                    step.httpsProxy);
            builder.perform(build, ws, launcher, listener, realToken);
            return null;
        }

        private String getTokenToUse() {
            if (step.tokenCredentialId != null && !step.tokenCredentialId.isEmpty()) {
                StringCredentials credentials = lookupCredentials();
                if (credentials != null) {
                    return credentials.getSecret().getPlainText();
                }
            }
            return step.token;
        }

        private StringCredentials lookupCredentials() {
            List<StringCredentials> credentials = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
            CredentialsMatcher matcher = CredentialsMatchers.withId(step.tokenCredentialId);
            return CredentialsMatchers.firstOrNull(credentials, matcher);
        }
    }
}
