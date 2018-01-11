package io.snyk.plugins;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.tasks.ArtifactArchiver;
import hudson.util.ArgumentListBuilder;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.*;

public class SnykSecurityBuilder extends Builder {

    private final String onFailBuild;
    private boolean isMonitor;
    private String targetFile = "";
    private String organization = "";
    private String envFlags="";
    private String dockerImage="";
    private String projectName="";
    private String httpProxy="";
    private String httpsProxy="";

    @DataBoundConstructor
    public SnykSecurityBuilder(String onFailBuild, boolean isMonitor, String targetFile, String organization, String envFlags, String dockerImage, String projectName, String httpProxy, String httpsProxy) {
        this.onFailBuild = onFailBuild;
        this.isMonitor = isMonitor;
        this.targetFile = targetFile;
        this.organization = organization;
        this.envFlags = envFlags;
        this.dockerImage = dockerImage;
        this.projectName = projectName;
        this.httpProxy = httpProxy;
        this.httpsProxy = httpsProxy;
    }

    public String isOnFailBuild(String state) {
        if (this.onFailBuild == null) {
            return "true".equals(state) ? "true" : "false";
        } else {
            return this.onFailBuild.equals(state) ? "true" : "false";
        }
    }

    public String getOnFailBuild() {
        return this.onFailBuild;
    }

    @DataBoundSetter
    public void setIsMonitor(boolean isMonitor) {
        this.isMonitor = isMonitor;
    }

    public boolean getIsMonitor() {
        return this.isMonitor;
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

    public String getEnvFlags() {
        return this.envFlags;
    }
    @DataBoundSetter
    public void setEnvFlags(String envFlags) {
        this.envFlags = envFlags;
    }

    public String getDockerImage() {
        return this.dockerImage;
    }
    @DataBoundSetter
    public void setDockerImage(String dockerImage) {
        this.dockerImage= dockerImage;
    }

    public String getProjectName() {
        return this.projectName;
    }
    @DataBoundSetter
    public void setProjectName(String projectName) {
        this.projectName= projectName;
    }

    public String getHttpProxy() {
        return this.httpProxy;
    }
    @DataBoundSetter
    public void setHttpProxy(String httpProxy) {
        this.httpProxy= httpProxy;
    }

    public String getHttpsProxy() {
        return this.httpsProxy;
    }
    @DataBoundSetter
    public void setHttpsProxy(String httpsProxy) {
        this.httpsProxy= httpsProxy;
    }

    @Override
    @SuppressFBWarnings({"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, java.lang.InterruptedException{
        String token;
        try {
            EnvVars envVars = build.getEnvironment(listener);
            token = envVars.get("SNYK_TOKEN");
            if (token == null){
                listener.getLogger().println("SNYK_TOKEN wasn't found");
                build.setResult(Result.FAILURE);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            build.setResult(Result.FAILURE);
            return false;
        }

        Result scanResult = scanProject(build, build.getWorkspace(), launcher, listener, token);
        build.setResult(scanResult);
        if (scanResult == Result.SUCCESS) {
            return true;
        }
        return false;
    }

    // From pipeline
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener,
                        @Nonnull String token) throws Exception {

        Result scanResult = scanProject(run, workspace, launcher, listener, token);
        if (scanResult == Result.FAILURE) {
            throw new Exception("Snyk returned failure");
        }
    }

    @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "REC_CATCH_EXCEPTION","OS_OPEN_STREAM"})
    public String getUserId(@Nonnull Launcher launcher, @Nonnull TaskListener listener) {
        String userId = "1000";

        try {
            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add("id", "-u");
            Launcher.ProcStarter ps = launcher.launch();
            ps.cmds(args);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            ps.stdout(baos);
            ps.quiet(true);
            int exitCode = ps.join();
            String input = new String(baos.toByteArray(), "UTF-8");
            if (exitCode != 0) {
                listener.getLogger().println("Failed to fetch userId using default: " + userId);
                return userId;
            }
            userId = input.replace("\n", "");
            userId = userId.replace(" ", "");
            listener.getLogger().println("Jenkins User ID: " + userId);
        } catch (Exception e) {
            listener.getLogger().println("Exception raised while getting group ID, using default value: " + userId);
        }
        return userId;
    }

    @SuppressFBWarnings({"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
    public Result scanProject(@Nonnull Run<?, ?> run,
                              @Nonnull FilePath workspace,
                              @Nonnull Launcher launcher,
                              @Nonnull TaskListener listener,
                              @Nonnull String token) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        String dirPath = workspace.toURI().getPath();
        String projectDirName = Paths.get(dirPath).getFileName().toString();
        String userId = getUserId(launcher, listener);
        args.add("docker", "run");
        args.add("--rm", "-v", "/var/run/docker.sock:/var/run/docker.sock");
        args.add("-e", "SNYK_TOKEN=" + token);
        if (this.isMonitor) {
            args.add("-e", "MONITOR=true");
        }
        if ((this.organization != null) && (!this.organization.equals(""))) {
            args.add("-e", "ORGANIZATION=" + this.organization);
        }
        if ((this.targetFile != null) && (!this.targetFile.equals(""))) {
            args.add("-e", "TARGET_FILE=" + this.targetFile);
        }
        if ((this.envFlags != null) && (!this.envFlags.equals(""))) {
            args.add("-e", "ENV_FLAGS=" + this.envFlags);
        }

        if ((this.projectName != null) && (!this.projectName.equals(""))) {
            args.add("-e", "PROJECT_FOLDER=" + this.projectName);
            projectDirName = this.projectName;
        } else{
            args.add("-e", "PROJECT_FOLDER=" + projectDirName);
        }

        if ((this.httpProxy != null) && (!this.httpProxy.equals(""))) {
            args.add("-e", "HTTP_PROXY=" + this.httpProxy);
        }

        if ((this.httpsProxy != null) && (!this.httpsProxy.equals(""))) {
            args.add("-e", "HTTPS_PROXY=" + this.httpsProxy);
        }

        String tempDir;
        if (new File("/tmp").exists()) {
            tempDir = "/tmp";
        } else {
            tempDir = System.getProperty("java.io.tmpdir");
        }

        EnvVars envVars = run.getEnvironment(listener);
        args.add("-e", "USER_ID="+userId);

        String javaRepo = envVars.get("MAVEN_REPO_PATH");
        String ivyRepo = envVars.get("IVY_REPO_PATH");

        if  ((ivyRepo != null) && (!ivyRepo.isEmpty())) {
            args.add("-v", ivyRepo + ":/home/node/.ivy2");
        }

        if ((javaRepo != null) && (!javaRepo.isEmpty())) {
            args.add("-v", javaRepo + ":/home/node/.m2");
        }

        String snykDockerImage = "snyk/snyk-cli";
        if ((this.dockerImage != null) && (!this.dockerImage.equals(""))) {
            snykDockerImage = dockerImage;
        }

        args.add("-v", dirPath + ":/project/" + projectDirName, "-v", tempDir + ":/tmp", snykDockerImage, "test", "--json");
        Launcher.ProcStarter ps = launcher.launch();
        ps.cmds(args);
        String command = args.toString();
        listener.getLogger().println(command.replace(token, "*****"));
        ps.stdin(null);
        ps.stderr(listener.getLogger());
        ps.stdout(listener.getLogger());
        ps.quiet(true);
        int exitCode = ps.join();
        listener.getLogger().println("exit code " + String.valueOf(exitCode));
        if (exitCode > 1) {
            return Result.FAILURE;
        }

        String artifactName = projectDirName + "_snyk_report.html";
        String artifactPath = dirPath + "/" + artifactName;
        String originalArtifactName = "/snyk_report.html";
        String originalArtifactPath = dirPath + "/" + originalArtifactName;
        
        Files.move(
            (new File(originalArtifactPath)).toPath(), 
            (new File(artifactPath)).toPath(),
            REPLACE_EXISTING
        );

        if (run.getActions(SnykSecurityAction.class).size() <= 0) {
            run.addAction(new SnykSecurityAction(run, artifactName));
        }

        archiveArtifacts(run, launcher, listener, workspace);

        if ((exitCode != 0) && (this.getOnFailBuild().equals("true"))) {
            return Result.FAILURE;
        }

        return Result.SUCCESS;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    private void archiveArtifacts(Run<?,?> build, Launcher launcher, TaskListener listener, FilePath workspace )
            throws java.lang.InterruptedException {
        ArtifactArchiver artifactArchiver = new ArtifactArchiver("*snyk_report.*");
        artifactArchiver.perform(build, workspace, launcher, listener);
    };

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Snyk Security";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }
    }
}

