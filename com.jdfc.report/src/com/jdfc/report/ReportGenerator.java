package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportGenerator {

    private static String DEFAULT = "default";

    // TODO: Create HTML Report
    public static void createReport(String workDir) {
        File exportDir = new File(workDir);
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        ExecutionDataNode<ExecutionData> root = CoverageDataStore.getInstance().getRoot();
        try {
            Map<String, ExecutionData> packageExecutionData = createHTMLFilesRecursive(root, null, workDir);
            HTMLGenerator.generateIndexFiles(packageExecutionData, workDir, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        File index = new File(String.format("%s/index.html", workDir));
//        File something = new File(String.format("%s/something.html", workDir));
//
//        try {
//            Writer writer = new FileWriter(index);
//            writeHTMLRecursive(writer, root);
//            writer.close();
//            Writer someWriter = new FileWriter(something);
//            someWriter.write("Its something!");
//            someWriter.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private static Map<String, ExecutionData> createHTMLFilesRecursive(ExecutionDataNode<ExecutionData> pNode, String pPathName, String workDir)
            throws IOException {
        String dir;
        String currentName;
        Map<String, ExecutionData> packageExecutionDataMap = new HashMap<>();
        if (pNode.isRoot()) {
            currentName = DEFAULT;
        } else {
            currentName = pPathName;
        }
        dir = String.format("%s/%s", workDir, currentName);

        File outputFolder = new File(dir);
        Map<String, ExecutionDataNode<ExecutionData>> children = pNode.getChildren();
        Map<String, ExecutionData> classExecutionDataMap = new HashMap<>();
        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : children.entrySet()) {
            if (entry.getValue().isLeaf()) {
                classExecutionDataMap.put(entry.getKey(), entry.getValue().getData());
                packageExecutionDataMap.put(currentName, pNode.getData());
                if(outputFolder.mkdir()){
                    // method overview
                    String overviewName = String.format("%s/%s.html", dir, entry.getKey());
                    File overview = new File(overviewName);
                    overview.createNewFile();

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
            HTMLGenerator.generateIndexFiles(classExecutionDataMap, dir, false);
        }
        return packageExecutionDataMap;
    }

    private static Map<String, ExecutionData> mergeMaps(Map<String, ExecutionData> map1, Map<String, ExecutionData> map2) {
        return Stream.of(map1, map2)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}
