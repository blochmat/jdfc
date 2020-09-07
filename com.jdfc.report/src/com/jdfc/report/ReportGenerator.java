package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.CoverageDataStore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportGenerator {

    // TODO: Create HTML Report
    public static void createReport(String workDir) {
        File exportDir = new File(workDir);
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        ExecutionDataNode<ExecutionData> root = CoverageDataStore.getInstance().getRoot();
        try {
            Map<String, ExecutionDataNode<ExecutionData>> packageExecutionData = createHTMLFilesRecursive(root, null, workDir);
            HTMLFactory.generateIndexFiles(packageExecutionData, workDir, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, ExecutionDataNode<ExecutionData>> createHTMLFilesRecursive(ExecutionDataNode<ExecutionData> pNode, String pPathName, String workDir)
            throws IOException {
        String dir;
        Map<String, ExecutionDataNode<ExecutionData>> packageExecutionDataMap = new HashMap<>();
        dir = String.format("%s/%s", workDir, pPathName);

        File outputFolder = new File(dir);
        Map<String, ExecutionDataNode<ExecutionData>> children = pNode.getChildren();
        Map<String, ExecutionDataNode<ExecutionData>> classExecutionDataMap = new HashMap<>();

        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : children.entrySet()) {
            if (entry.getValue().isLeaf()) {
                classExecutionDataMap.put(entry.getKey(), entry.getValue());
                packageExecutionDataMap.put(pPathName, pNode);

                if(outputFolder.mkdir() || outputFolder.exists()){
                    // method overview
                    String overviewName = String.format("%s/%s.html", dir, entry.getKey());
                    File overView = new File(overviewName);
                    overView.createNewFile();
                    HTMLFactory.createClassOverview(entry.getKey(), entry.getValue().getData(), dir);

                    // class detail view
                    String detailViewName = String.format("%s/%s.java.html", dir, entry.getKey());
                    File detailView = new File(detailViewName);
                    detailView.createNewFile();
                }
            } else {
                String nextPathName;

                if (pPathName == null) {
                    nextPathName = entry.getKey();
                } else {
                    nextPathName = String.format("%s.%s", pPathName, entry.getKey());
                }

                packageExecutionDataMap = mergeMaps(packageExecutionDataMap,
                        createHTMLFilesRecursive(entry.getValue(), nextPathName, workDir));
            }
        }
        if (outputFolder.exists()) {
            HTMLFactory.generateIndexFiles(classExecutionDataMap, dir, false);
        }
        return packageExecutionDataMap;
    }

    private static Map<String, ExecutionDataNode<ExecutionData>> mergeMaps(Map<String, ExecutionDataNode<ExecutionData>> map1, Map<String, ExecutionDataNode<ExecutionData>> map2) {
        return Stream.of(map1, map2)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}
