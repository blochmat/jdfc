package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.report.html.HTMLFactory;
import com.jdfc.report.html.resources.Resources;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportGenerator {

    public void createReport(final String pExportDir, final String pSourceDir) {
        File jdfcReportDir = new File(pExportDir);
        if (!jdfcReportDir.exists()) {
            jdfcReportDir.mkdir();
        }
        Resources resources = new Resources(jdfcReportDir);
        ExecutionDataNode<ExecutionData> root = CoverageDataStore.getInstance().getRoot();
        try {
            resources.copyResource();
            Map<String, ExecutionDataNode<ExecutionData>> packageExecutionData = createHTMLFilesRecursive(
                    root, null, pExportDir, pSourceDir, resources);
            HTMLFactory.generateIndexFiles(packageExecutionData, pExportDir, resources);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, ExecutionDataNode<ExecutionData>> createHTMLFilesRecursive(
            final ExecutionDataNode<ExecutionData> pNode,
            final String pPathName,
            final String pExportDir,
            final String pSourceDir,
            final Resources pResources)
            throws IOException {
        String dir;
        Map<String, ExecutionDataNode<ExecutionData>> packageExecutionDataMap = new HashMap<>();
        dir = String.format("%s/%s", pExportDir, pPathName);

        File outputFolder = new File(dir);
        Map<String, ExecutionDataNode<ExecutionData>> children = pNode.getChildren();
        Map<String, ExecutionDataNode<ExecutionData>> classExecutionDataMap = new HashMap<>();

        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : children.entrySet()) {
            if (entry.getValue().isLeaf()) {
                classExecutionDataMap.put(entry.getKey(), entry.getValue());
                packageExecutionDataMap.put(pPathName, pNode);

                if (outputFolder.mkdir() || outputFolder.exists()) {
                    // method overview
                    HTMLFactory.createClassOverview(entry.getKey(), entry.getValue().getData(), dir, pResources);

                    // class detail view
                    HTMLFactory.createClassDetailView(entry.getKey(), entry.getValue().getData(), dir,
                            pSourceDir, pResources);
                    ClassExecutionData pData = (ClassExecutionData) entry.getValue().getData();
                    System.out.println(new PrettyPrintMap<>(pData.getDefUsePairs()));
                }
            } else {
                String nextPathName;

                if (pPathName == null) {
                    nextPathName = entry.getKey();
                } else {
                    nextPathName = String.format("%s.%s", pPathName, entry.getKey());
                }

                packageExecutionDataMap = mergeMaps(packageExecutionDataMap,
                        createHTMLFilesRecursive(entry.getValue(), nextPathName, pExportDir, pSourceDir, pResources));
            }
        }
        if (outputFolder.exists()) {
            HTMLFactory.generateIndexFiles(classExecutionDataMap, dir, pResources);
        }
        return packageExecutionDataMap;
    }

    private Map<String, ExecutionDataNode<ExecutionData>> mergeMaps(Map<String, ExecutionDataNode<ExecutionData>> map1, Map<String, ExecutionDataNode<ExecutionData>> map2) {
        return Stream.of(map1, map2)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}
