package com.jdbc.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.Properties;

public abstract class AbstractAgentMojo extends AbstractJdfcMojo{

    abstract File getDestFile();

    @Override
    protected void executeMojo() throws MojoExecutionException, MojoFailureException {
        String name = "argLine";
        Properties projectProperties = getProject().getProperties();
        String odlValue = projectProperties.getProperty(name);
        // create command line arguments for agent
        String newValue = createArgLine();
        projectProperties.setProperty(name, newValue);

    }

    private String createArgLine(){
        return  "asdf";
    }
}
