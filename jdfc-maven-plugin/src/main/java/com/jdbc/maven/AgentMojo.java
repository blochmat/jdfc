package com.jdbc.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AgentMojo extends AbstractAgentMojo {
    @Parameter(property = "jdfc.destFile", defaultValue = "${project.build.directory}/jdfc.exec")
    private File destFile;

    @Override
    public File getDestFile(){
        return destFile;
    }
}
