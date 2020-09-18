package com.jdfc.maven;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.data.PackageExecutionData;
import com.jdfc.report.LoadController;
import com.jdfc.report.ReportGenerator;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.io.*;
import java.util.*;


@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ReportMojo extends AbstractMavenReport {

    @Parameter(defaultValue = "${project.reporting.outputDirectory}/jdfc")
    private File outputDirectory;

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Override
    protected Renderer getSiteRenderer() {
        return null;
    }

    @Override
    protected String getOutputDirectory() {
        return null;
    }

    @Override
    protected MavenProject getProject() {
        return this.project;
    }

    @Override
    public void execute() {
        try {
            executeReport(Locale.getDefault());
        } catch (MavenReportException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        final String target = getProject().getBuild().getDirectory();
        final String source = getProject().getBuild().getSourceDirectory();
        final String importDir = String.format("%s/jdfc", target);

        // Load stored data into CoverageDataStore
        LoadController.loadDataFromXML(importDir);

        // TODO: REMOVE DEBUG
        debugPrintChildren(CoverageDataStore.getInstance().getRoot(), 1);
        final String resourcesDir = String.format("%s/jdfc-resources", target);
        try {
            // TODO: Add more style or store a static file
            ReportGenerator.createReportCSS(resourcesDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String exportDir = String.format("%s/jdfc-report", target);
        ReportGenerator.createReport(exportDir, source);
    }

    private void debugPrintChildren(ExecutionDataNode<ExecutionData> pNode, int indent) {
        if (pNode.isRoot()) {
            PackageExecutionData rootData = (PackageExecutionData) pNode.getData();
            String root = String.format("root %s %s %s %s", rootData.getMethodCount(), rootData.getTotal(), rootData.getCovered(), rootData.getMissed());
            System.out.println(root);
        }

        Map<String, ExecutionDataNode<ExecutionData>> map = pNode.getChildren();
        String strip = "";
        for (int i = 0; i < indent; i++) {
            strip = strip.concat("- ");
        }
        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : map.entrySet()) {
            ExecutionData data = entry.getValue().getData();
            String str = String.format("%s%s %s %s %s %s", strip,
                    entry.getKey(), data.getMethodCount(), data.getTotal(), data.getCovered(), data.getMissed());
            System.out.println(str);
            debugPrintChildren(entry.getValue(), indent + 1);
        }
    }

    @Override
    public String getOutputName() {
        return null;
    }

    @Override
    public String getName(Locale locale) {
        return null;
    }

    @Override
    public String getDescription(Locale locale) {
        return null;
    }
}
