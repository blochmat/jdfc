package mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static java.lang.String.format;

@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AgentMojo extends AbstractJdfcMojo {

    /**
     * Map of plugin artifacts.
     */
    @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
    public Map<String, Artifact> pluginArtifactMap;

    private static final String AGENT_FILE_NAME = "com.jdfc.agent-1.0-SNAPSHOT-runtime.jar";

    /**
     * When executing the mojo we add the agent argument to the command line.
     * If ths seems unnecessarily complicated to you then you might be right.
     *
     */
    @Override
    protected void executeMojo()  {
        getLog().info("Preparing JDFC agent for analysis. ");
        final String targetDirStr = getProject().getBuild().getDirectory(); // target
        final String classesDirStr = getProject().getBuild().getOutputDirectory(); // target/classes
        final String argLine = "argLine";
        try {
            final File agentJarFile = extractAgentJarToDir(targetDirStr);

            if (agentJarFile != null) {
                final Properties projectProperties = getProject().getProperties();
                final String oldValue = projectProperties.getProperty(argLine);
                // create command line arguments for agent
                final String agent = format("-javaagent:%s", agentJarFile);
                String newValue;
                if (oldValue == null) {
                    newValue = String.format("%s=%s", agent, classesDirStr);
                } else {
                    newValue = String.format("%s %s=%s", oldValue, agent, classesDirStr);
                }
                projectProperties.setProperty(argLine, newValue);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public File copyJarFile(File jarFile) throws IOException {
        File copyJarFile = new File(getProject().getBasedir(), jarFile.getName());
        FileOutputStream fileOutputStream = new FileOutputStream(copyJarFile);
        FileInputStream fileInputStream = new FileInputStream(jarFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fileInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, length);
        }
        fileInputStream.close();
        fileOutputStream.close();
        return copyJarFile;
    }

    public File extractAgentJarToDir(String dirStr) throws Exception {
        final Artifact pluginArtifact = pluginArtifactMap.get("com.jdfc:jdfc-maven-plugin");
        final File targetDir = new File(dirStr);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        final File pluginJarFile = copyJarFile(pluginArtifact.getFile());

        JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(pluginJarFile.toPath()));
        JarEntry jarEntry;
        File file = null;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (jarEntry.getName().equals(AGENT_FILE_NAME)) {
                file = new File(dirStr, AGENT_FILE_NAME);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = jarInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
                fileOutputStream.close();
                break;
            }
        }
        jarInputStream.close();
        if(!pluginJarFile.delete()) {
            throw new RuntimeException("Deletion of Plugin Jar File failed.");
        }
        return file;
    }
}
