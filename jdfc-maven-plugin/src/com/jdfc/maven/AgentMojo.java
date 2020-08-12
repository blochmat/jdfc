package com.jdfc.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;

@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AgentMojo extends AbstractJdfcMojo{

    @Parameter(property = "jdfc.destFile", defaultValue = "${project.build.directory}/jdfc.exec")
    private File destFile;

    /**
     * Map of plugin artifacts.
     */
    @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
    Map<String, Artifact> pluginArtifactMap;

    /**
     * Name of the JaCoCo Agent artifact.
     */
    static final String AGENT_ARTIFACT_NAME = "com.jdfc:com.jdfc.agent";

    @Override
    protected void executeMojo() throws MojoExecutionException, MojoFailureException {
        String name = "argLine";
        Properties projectProperties = getProject().getProperties();
        String oldValue = projectProperties.getProperty(name);
        // create command line arguments for agent
        final String plainAgent = format("-javaagent:%s", getAgentJarFile());
        String newValue = format("%s", plainAgent);
        projectProperties.setProperty(name, newValue);
    }

    File getAgentJarFile() {
        final Artifact jdfcAgentArtifact = pluginArtifactMap
                .get(AGENT_ARTIFACT_NAME);
        return jdfcAgentArtifact.getFile();
    }

    public File getDestFile(){
        return destFile;
    }
}
