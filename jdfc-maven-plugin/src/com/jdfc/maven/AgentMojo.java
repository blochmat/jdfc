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
public class AgentMojo extends AbstractJdfcMojo {

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
        String argLine = "argLine";
        Properties projectProperties = getProject().getProperties();
        String oldValue = projectProperties.getProperty(argLine);
        // create command line arguments for agent
        final String baseDir = getProject().getBuild().getOutputDirectory();
        final String agent = format("-javaagent:%s", getAgentJarFile());
        String newValue = "";
        if (oldValue == null) {
            newValue = format("%s=%s", agent, baseDir);
        } else {
            newValue = format("%s%s=%s", oldValue, agent, baseDir);
        }
        projectProperties.setProperty(argLine, newValue);
    }

    File getAgentJarFile() {
        final Artifact jdfcAgentArtifact = pluginArtifactMap
                .get(AGENT_ARTIFACT_NAME);
        return jdfcAgentArtifact.getFile();
    }
}
