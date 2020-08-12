package com.jdfc.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractJdfcMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(property = "jdfc.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping JDFC execution, because jdfc.skip is set.");
            skipMojo();
            return;
        }
        executeMojo();
    }

    /**
     * Skips Mojo.
     */
    protected void skipMojo() {
    }

    /**
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected abstract void executeMojo() throws MojoExecutionException, MojoFailureException;

    /**
     * @return Maven Project
     */
    protected final MavenProject getProject() {
        return this.project;
    }
}
