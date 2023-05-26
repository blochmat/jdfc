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
    private Map<String, Artifact> pluginArtifactMap;

    @Parameter(defaultValue = "${project.build.directory}")
    private String targetDirString;

    static final String AGENT_FILE_NAME = "com.jdfc.agent-1.0-SNAPSHOT-runtime.jar";

    /**
     * When executing the mojo we add the agent argument to the command line.
     * If ths seems unnecessarily complicated to you then you might be right.
     *
     */
    @Override
    protected void executeMojo()  {
        getLog().info("Preparing JDFC agent for analysis. ");
        final String argLine = "argLine";
        final Artifact pluginArtifact = pluginArtifactMap.get("com.jdfc:jdfc-maven-plugin");
        try {
            final File targetDir = new File(targetDirString);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            final File pluginJarFile = copyJarFile(pluginArtifact.getFile());
            final File agentJarFile = extractAgentJarToTargetDir(pluginJarFile, AGENT_FILE_NAME, targetDir);
            if (agentJarFile != null) {
                final Properties projectProperties = getProject().getProperties();
                final String oldValue = projectProperties.getProperty(argLine);
                // create command line arguments for agent
                final String agent = format("-javaagent:%s", agentJarFile);
                final File classesDir = new File(getProject().getBuild().getOutputDirectory());
                String newValue;
                if (oldValue == null) {
                    newValue = String.format("%s=%s", agent, classesDir);
                } else {
                    newValue = String.format("%s %s=%s", oldValue, agent, classesDir);
                }
                projectProperties.setProperty(argLine, newValue);
                pluginJarFile.delete();
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

    public File extractAgentJarToTargetDir(File jarFile, String fileName, File destDirectory) throws Exception {
        JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(jarFile.toPath()));
        JarEntry jarEntry;
        File file = null;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (jarEntry.getName().equals(fileName)) {
                file = new File(destDirectory, fileName);
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
        return file;
    }
}
