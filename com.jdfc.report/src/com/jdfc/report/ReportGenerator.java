package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.CoverageDataStore;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportGenerator {

    public static void createReport(String exportDir, String sourceDir) {
        File jdfcReportDir = new File(exportDir);
        if (!jdfcReportDir.exists()) {
            jdfcReportDir.mkdir();
        }
        ExecutionDataNode<ExecutionData> root = CoverageDataStore.getInstance().getRoot();
        try {
            Map<String, ExecutionDataNode<ExecutionData>> packageExecutionData = createHTMLFilesRecursive(
                    root, null, exportDir, sourceDir);
            HTMLFactory.generateIndexFiles(packageExecutionData, exportDir, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, ExecutionDataNode<ExecutionData>> createHTMLFilesRecursive(
            ExecutionDataNode<ExecutionData> pNode, String pPathName, String exportDir, String sourceDir)
            throws IOException {
        String dir;
        Map<String, ExecutionDataNode<ExecutionData>> packageExecutionDataMap = new HashMap<>();
        dir = String.format("%s/%s", exportDir, pPathName);

        File outputFolder = new File(dir);
        Map<String, ExecutionDataNode<ExecutionData>> children = pNode.getChildren();
        Map<String, ExecutionDataNode<ExecutionData>> classExecutionDataMap = new HashMap<>();

        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : children.entrySet()) {
            if (entry.getValue().isLeaf()) {
                classExecutionDataMap.put(entry.getKey(), entry.getValue());
                packageExecutionDataMap.put(pPathName, pNode);

                if (outputFolder.mkdir() || outputFolder.exists()) {
                    // method overview
                    HTMLFactory.createClassOverview(entry.getKey(), entry.getValue().getData(), dir);

                    // class detail view
                    HTMLFactory.createClassDetailView(entry.getKey(), entry.getValue().getData(), dir, sourceDir);
                }
            } else {
                String nextPathName;

                if (pPathName == null) {
                    nextPathName = entry.getKey();
                } else {
                    nextPathName = String.format("%s.%s", pPathName, entry.getKey());
                }

                packageExecutionDataMap = mergeMaps(packageExecutionDataMap,
                        createHTMLFilesRecursive(entry.getValue(), nextPathName, exportDir, sourceDir));
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

    // TODO: Change resource file creation
    public static void createReportCSS(String pOutputDir) throws IOException {
        File outputDir = new File(pOutputDir);
        outputDir.mkdir();
        File target = new File(pOutputDir + "/report.css");
        FileWriter writer = new FileWriter(target);
        writer.write(".style-class {\n" +
                "    font-family: SFMono-Regular,Consolas,Liberation Mono,Menlo,monospace;\n" +
                "}");
        writer.write("h1 {\n" +
                "  font-weight:bold;\n" +
                "  font-size:18pt;\n" +
                "}");
        writer.write(".tooltip {\n" +
                "  position: relative;\n" +
                "  display: inline-block;\n" +
                "  border-bottom: 1px dotted black; /* If you want dots under the hoverable text */\n" +
                "}\n" +
                "\n" +
                "/* Tooltip text */\n" +
                ".tooltip .tooltiptext {\n" +
                "  visibility: hidden;\n" +
                "  width: 120px;\n" +
                "  background-color: black;\n" +
                "  color: #fff;\n" +
                "  text-align: center;\n" +
                "  padding: 5px 0;\n" +
                "  border-radius: 6px;\n" +
                " \n" +
                "  /* Position the tooltip text - see examples below! */\n" +
                "  position: absolute;\n" +
                "  z-index: 1;\n" +
                "  top: -5px;\n" +
                "  left: 120%;\n" +
                "}\n" +
                "\n" +
                "/* Show the tooltip text when you mouse over the tooltip container */\n" +
                ".tooltip:hover .tooltiptext {\n" +
                "  visibility: visible;\n" +
                "}");
        writer.close();
    }
}
