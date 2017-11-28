package io.snyk.plugins;

import com.google.common.io.Files;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SnykSecurityBuilderTest {
    private SnykSecurityBuilder snykBuilder;

    @Before
    public void setup() {
        String onFailBuild = null;
        boolean isMonitor = false;
        String targetFile = null;
        String organization = null;
        String envFlags = null;
        String dockerImage = null;
        String projectName = null;

        snykBuilder = new SnykSecurityBuilder(onFailBuild, isMonitor, targetFile, organization, envFlags, dockerImage, projectName);
    }

    @Test
    public void scanDefaultWorkspaceIfNoProjectFolderIsGiven() throws Exception {
        FilePath workspace = new FilePath(Files.createTempDir());

        Map<String, String> dockerEnv = testProjectName(workspace);

        System.out.println(dockerEnv.get("PROJECT_FOLDER"));

        assertEquals(workspace.getName(), dockerEnv.get("PROJECT_FOLDER"));
    }

    @Test
    public void builderShouldSetProjectFolderWhenProjectNameIsGiven() throws Exception {
        final String projectName = "subfolder/MySubProject";
        snykBuilder.setProjectName(projectName);

        Map<String, String> dockerEnv = testProjectName(new FilePath(Files.createTempDir()));

        assertEquals(projectName, dockerEnv.get("PROJECT_FOLDER"));
    }

    @Test
    public void givenNoProjectNameWorkspaceShouldBeMappedToProjectDirectory() throws Exception {
        FilePath workspace = new FilePath(Files.createTempDir());
        ArgumentListBuilder args = testBuilder(workspace);

        Map<String, String> volumeMappings = getVolumeMappings(args.toList());

        boolean found = false;
        for (Map.Entry e : volumeMappings.entrySet()) {
            if (e.getKey().equals(workspace.toURI().getPath()) && e.getValue().equals("/project/")) {
                found = true;
            }
        }

        assertTrue("Workspace should be mapped to project directory", found);
    }

    @Test
    public void projectNameDirectoryShouldBeMappedToToProjectDirectory() throws Exception {
        FilePath workspace = new FilePath(Files.createTempDir());
        final String projectName = "my/super/project";
        snykBuilder.setProjectName(projectName);
        ArgumentListBuilder args = testBuilder(workspace);

        Map<String, String> volumeMappings = getVolumeMappings(args.toList());
        Map<String, String> environmentVars = getEnvironmentVariables(args.toList());

        // Ensure that the PROJECT_FOLDER is set since snyk will look for this inside the /project/ directory
        String projectFolder = environmentVars.get("PROJECT_FOLDER");
        assertEquals(projectName, projectFolder);

        boolean found = false;
        for (Map.Entry<String, String> v : volumeMappings.entrySet()) {
            String ws = workspace.toURI().getPath();
            // Host directory
            String key = v.getKey();
            // Container directory
            String value = v.getValue();

            /*
            Either you map the /workspacePath/subfolder/to/project/ to the docker scan root /project/
            or you map /workspacePath/ to /project/ and specify the path by using the PROJECT_FOLDER environment
            variable
            */
            if (key.equals(ws + projectName) && value.equals("/project/") && (projectFolder == null || projectFolder.equals("/"))) {
                found = true;
            } else if (key.equals(ws) && value.equals("/project/") && (projectFolder != null && projectFolder.equals(projectName))) {
                found = true;
            }
        }

        assertTrue("Workspace should be mapped to project directory. Found " + volumeMappings.toString(), found);
    }

    private ArgumentListBuilder testBuilder(FilePath workspace) throws Exception {
        //
        // Setup
        //
        Run<?, ?> run = mock(Run.class);

        Launcher launcher = mock(Launcher.class);
        Launcher.ProcStarter procLauncherMock = mock(Launcher.ProcStarter.class);
        when(launcher.launch()).thenReturn(procLauncherMock);

        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(System.out);

        EnvVars envVars = new EnvVars();
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(envVars);

        String token = "123";

        //
        // Execution
        //
        Result result = snykBuilder.scanProject(run, workspace, launcher, listener, token);

        //
        // Verification
        //
        ArgumentCaptor<ArgumentListBuilder> argumentCaptor = ArgumentCaptor.forClass(ArgumentListBuilder.class);
        verify(procLauncherMock, atLeastOnce()).cmds(argumentCaptor.capture());

        ArgumentListBuilder args = argumentCaptor.getValue();

        assertEquals(Result.SUCCESS, result);

        return args;
    }

    private Map<String, String> testProjectName(FilePath workspace) throws Exception {
        ArgumentListBuilder args = testBuilder(workspace);

        return getEnvironmentVariables(args.toList());
    }

    private static Map<String, String> getEnvironmentVariables(List<String> list) {
        return getSplitMap(getVariables(list, "-e"), "=");
    }

    private static Map<String, String> getVolumeMappings(List<String> list) {
        return getSplitMap(getVariables(list, "-v"), ":");
    }

    private static Map<String, String> getSplitMap(List<String> list, String splitChar) {
        Map<String, String> result = new HashMap<>();

        for (String e : list) {
            String[] split = e.split(splitChar);
            if (split.length == 1) {
                result.put(split[0], "");
            } else {
                result.put(split[0], split[1]);
            }
        }

        return result;
    }

    private static List<String> getVariables(List<String> list, String needle) {
        List<String> env = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            String flag = list.get(i);

            if (flag.equals(needle)) {
                env.add(list.get(i + 1));
            }
        }

        return env;
    }
}