package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;
import com.jdfc.report.html.HTMLFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;


// TODO: pro/contra static methods
public class HTMLFactory {

    public static void generateIndexFiles(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                          String pWorkDir,
                                          boolean isRoot) throws IOException {
        String indexPath = String.format("%s/index.html", pWorkDir);
        File index = new File(indexPath);
        Writer writer = new FileWriter(index);
        writer.write(createIndexHTML(pClassFileDataMap));
        writer.close();
        if(!isRoot){
            // TODO: Create indexSource files
            String indexSourcePath = String.format("%s/index.source.html", pWorkDir);
            File indexSource = new File(indexSourcePath);
            indexSource.createNewFile();
        }
    }

    private static String createIndexHTML(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap){
        HTMLFile html = new HTMLFile();

        // TODO: Add styles and meta information
        html.fillHeader("This is the header");

        // TODO: Implement HTML creation
        List<String> columns = new ArrayList<>(Arrays.asList("Element", "Method Count", "Total", "Covered", "Missed"));
        html.addTable(columns, pClassFileDataMap);
        return html.render();
    }

    public static void createClassOverview(String pClassName, ExecutionData pData, String pWorkDir) throws IOException {
        if(pData instanceof ClassExecutionData) {
            String filePath = String.format("%s/%s.html", pWorkDir, pClassName);
            File classOverview = new File(filePath);
            Writer writer = new FileWriter(classOverview);
            String content = createOverviewHTML((ClassExecutionData) pData);
            if (content != null) {
                writer.write(content);
            }
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    private static String createOverviewHTML(ClassExecutionData pData) {
        HTMLFile html = new HTMLFile();

        html.fillHeader("This is the header");

        List<String> columns = new ArrayList<>(Arrays.asList("Element", "Total", "Covered", "Missed"));
        html.addTable(columns, pData);
        return html.render();
    }

    public static void createClassDetailView(String pClassName, ExecutionData pData, String pWorkDir, String sourceDir)
            throws IOException {
        if(pData instanceof ClassExecutionData) {
            String filePath = String.format("%s/%s.java.html", pWorkDir, pClassName);
            File detailView = new File(filePath);

            String className = String.format("%s/%s.java", sourceDir, ((ClassExecutionData) pData).getRelativePath());
            File classFile = new File(className);
            Scanner scanner = new Scanner(classFile);
            String data = "";
            while(scanner.hasNextLine()){
                data = data.concat(scanner.nextLine()+"\n");
            }
            scanner.close();

            Writer writer = new FileWriter(detailView);
            String content = createDetailViewHTML(data, pData);
            if (content != null) {
                writer.write(content);
            }
            writer.close();
        }
        else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    private static String createDetailViewHTML(String pData, ExecutionData pExecutionData) {
        HTMLFile html = new HTMLFile();

        return html.render();
    }
}
