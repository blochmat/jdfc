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
        writer.write("table.coverage {\n" +
                "  empty-cells:show;\n" +
                "  border-collapse:collapse;\n" +
                "}\n" +
                "\n" +
                "table.coverage thead {\n" +
                "  background-color:#e0e0e0;\n" +
                "}\n" +
                "\n" +
                "table.coverage thead td {\n" +
                "  white-space:nowrap;\n" +
                "  padding:2px 14px 0px 6px;\n" +
                "  border-bottom:#b0b0b0 1px solid;\n" +
                "}\n" +
                "\n" +
                "table.coverage thead td.bar {\n" +
                "  border-left:#cccccc 1px solid;\n" +
                "}\n" +
                "\n" +
                "table.coverage thead td.ctr1 {\n" +
                "  text-align:right;\n" +
                "  border-left:#cccccc 1px solid;\n" +
                "}\n" +
                "\n" +
                "table.coverage thead td.ctr2 {\n" +
                "  text-align:right;\n" +
                "  padding-left:2px;\n" +
                "}\n" +
                "\n" +
                "table.coverage thead td.sortable {\n" +
                "  cursor:pointer;\n" +
                "  background-image:url(sort.gif);\n" +
                "  background-position:right center;\n" +
                "  background-repeat:no-repeat;\n" +
                "}\n" +
                "\n" +
                "table.coverage thead td.up {\n" +
                "  background-image:url(up.gif);\n" +
                "}\n" +
                "\n" +
                "table.coverage thead td.down {\n" +
                "  background-image:url(down.gif);\n" +
                "}\n" +
                "\n" +
                "table.coverage tbody td {\n" +
                "  white-space:nowrap;\n" +
                "  padding:2px 6px 2px 6px;\n" +
                "  border-bottom:#d6d3ce 1px solid;\n" +
                "}\n" +
                "\n" +
                "table.coverage tbody tr:hover {\n" +
                "  background: #f0f0d0 !important;\n" +
                "}\n" +
                "\n" +
                "table.coverage tbody td.bar {\n" +
                "  border-left:#e8e8e8 1px solid;\n" +
                "}\n" +
                "\n" +
                "table.coverage tbody td.ctr1 {\n" +
                "  text-align:right;\n" +
                "  padding-right:14px;\n" +
                "  border-left:#e8e8e8 1px solid;\n" +
                "}\n" +
                "\n" +
                "table.coverage tbody td.ctr2 {\n" +
                "  text-align:right;\n" +
                "  padding-right:14px;\n" +
                "  padding-left:2px;\n" +
                "}\n" +
                "\n" +
                "table.coverage tfoot td {\n" +
                "  white-space:nowrap;\n" +
                "  padding:2px 6px 2px 6px;\n" +
                "}\n" +
                "\n" +
                "table.coverage tfoot td.bar {\n" +
                "  border-left:#e8e8e8 1px solid;\n" +
                "}\n" +
                "\n" +
                "table.coverage tfoot td.ctr1 {\n" +
                "  text-align:right;\n" +
                "  padding-right:14px;\n" +
                "  border-left:#e8e8e8 1px solid;\n" +
                "}\n" +
                "\n" +
                "table.coverage tfoot td.ctr2 {\n" +
                "  text-align:right;\n" +
                "  padding-right:14px;\n" +
                "  padding-left:2px;\n" +
                "}\n" +
                "\n" +
                ".footer {\n" +
                "  margin-top:20px;\n" +
                "  border-top:#d6d3ce 1px solid;\n" +
                "  padding-top:2px;\n" +
                "  font-size:8pt;\n" +
                "  color:#a0a0a0;\n" +
                "}\n" +
                "\n" +
                ".footer a {\n" +
                "  color:#a0a0a0;\n" +
                "}");
        writer.close();
    }
}
