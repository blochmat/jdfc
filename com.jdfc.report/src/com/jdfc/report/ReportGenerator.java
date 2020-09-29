package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.report.html.HTMLFactory;
import com.jdfc.report.html.resources.Resources;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportGenerator {

    private final File reportDir;
    private final File sourceDir;
    private final HTMLFactory htmlFactory;

    public ReportGenerator(String pReportDir, String pSourceDir) {
        File reportDir = new File(pReportDir);
        if (!reportDir.exists()) {
            reportDir.mkdir();
        }
        this.reportDir = reportDir;
        this.sourceDir = new File(pSourceDir);
        Resources resources = new Resources(reportDir);
        this.htmlFactory = new HTMLFactory(resources, reportDir);
    }

    public void createReport() {
        ExecutionDataNode<ExecutionData> root = CoverageDataStore.getInstance().getRoot();
        try {
            Map<String, ExecutionDataNode<ExecutionData>> packageExecutionData = createHTMLFilesRecursive(
                    root, null);
            htmlFactory.generateIndexFiles(packageExecutionData, reportDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, ExecutionDataNode<ExecutionData>> createHTMLFilesRecursive(
            final ExecutionDataNode<ExecutionData> pNode,
            final String pPathName)
            throws IOException {
        String dir;
        Map<String, ExecutionDataNode<ExecutionData>> packageExecutionDataMap = new HashMap<>();
        dir = String.format("%s/%s", reportDir.toString(), pPathName);

        File outputFolder = new File(dir);
        Map<String, ExecutionDataNode<ExecutionData>> children = pNode.getChildren();
        Map<String, ExecutionDataNode<ExecutionData>> classExecutionDataMap = new HashMap<>();

        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : children.entrySet()) {
            if (entry.getValue().isLeaf()) {
                classExecutionDataMap.put(entry.getKey(), entry.getValue());
                packageExecutionDataMap.put(pPathName, pNode);

                if (outputFolder.mkdir() || outputFolder.exists()) {
                    // method overview
                    htmlFactory.createClassOverview(entry.getKey(), entry.getValue().getData(), outputFolder);

                    // class detail view
                    htmlFactory.createClassDetailView(entry.getKey(), entry.getValue().getData(), outputFolder,
                            sourceDir);
                }
            } else {
                String nextPathName;

                if (pPathName == null) {
                    nextPathName = entry.getKey();
                } else {
                    nextPathName = String.format("%s.%s", pPathName, entry.getKey());
                }

                packageExecutionDataMap = mergeMaps(packageExecutionDataMap,
                        createHTMLFilesRecursive(entry.getValue(), nextPathName));
            }
        }
        if (outputFolder.exists()) {
            htmlFactory.generateIndexFiles(classExecutionDataMap, outputFolder);
        }
        return packageExecutionDataMap;
    }

    private Map<String, ExecutionDataNode<ExecutionData>> mergeMaps(Map<String, ExecutionDataNode<ExecutionData>> map1,
                                                                    Map<String, ExecutionDataNode<ExecutionData>> map2) {
        return Stream.of(map1, map2)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}
