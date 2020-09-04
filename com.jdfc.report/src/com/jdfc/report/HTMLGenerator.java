package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class HTMLGenerator {

    public static void generateIndexFiles(Map<String, ExecutionData> pClassFileDataMap, String pWorkDir, boolean isRoot) throws IOException {
        String indexPath = String.format("%s/index.html", pWorkDir);
        File index = new File(indexPath);
        index.createNewFile();
        if(!isRoot){
            String indexSourcePath = String.format("%s/index.source.html", pWorkDir);
            File indexSource = new File(indexSourcePath);
            indexSource.createNewFile();
        }
    }

    private void writeHTMLRecursive(Writer pWriter, ExecutionDataNode<ExecutionData> pNode) throws IOException {
        pWriter.write(String.format("<!DOCTYPE html><html><head>%s</head><body>%s</body></html>",
                writeHeader(pWriter, pNode),
                writeBodyRecursive(pNode)));
    }

    // Content and coverage information
    private String writeBodyRecursive(ExecutionDataNode<ExecutionData> pNode) {
        String str = "";
        if (pNode.getData() instanceof ClassExecutionData) {
            // return class view with marked entries
        } else {
            // create Table view with entry for every child and link to the respective html
            Map<String, ExecutionDataNode<ExecutionData>> children = pNode.getChildren();
            for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : children.entrySet()) {
                str = str.concat(String.format("<h1>%s</h1>\n", entry.getKey()));
            }
        }
        return str;
    }


    // Style and header title
    private String writeHeader(Writer pWriter, ExecutionDataNode<ExecutionData> root) {
        return "<a href=\"something.html\">Some Link</a>";
    }
}
