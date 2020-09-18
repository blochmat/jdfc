package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.report.html.HTMLFile;
import com.jdfc.report.html.Span;
import com.jdfc.report.html.table.Row;
import com.jdfc.report.html.table.Table;

import javax.swing.*;
import java.io.*;
import java.util.*;


// TODO: pro/contra static methods
public class HTMLFactory {

    public static void generateIndexFiles(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                          String pWorkDir,
                                          boolean isRoot) throws IOException {
        String indexPath = String.format("%s/index.html", pWorkDir);
        File index = new File(indexPath);
        Writer writer = new FileWriter(index);
        writer.write(createIndexHTML(pClassFileDataMap, pWorkDir));
        writer.close();
        if (!isRoot) {
            // TODO: Create indexSource files?
            String indexSourcePath = String.format("%s/index.source.html", pWorkDir);
            File indexSource = new File(indexSourcePath);
            indexSource.createNewFile();
        }
    }

    private static String createIndexHTML(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap, String pWorkDir) {
        String[] split = pWorkDir.split("/");
        String title = split[split.length-1];

        HTMLFile html = new HTMLFile(title);
        html.createHeader();
        List<String> columns = new ArrayList<>(Arrays.asList("Element", "Method Count", "Total", "Covered", "Missed"));
        html.addTable(columns, pClassFileDataMap);
        return html.render();
    }

    public static void createClassOverview(String pClassName, ExecutionData pData, String pWorkDir) throws IOException {
        if (pData instanceof ClassExecutionData) {
            String filePath = String.format("%s/%s.html", pWorkDir, pClassName);
            File classOverview = new File(filePath);
            Writer writer = new FileWriter(classOverview);
            String content = createClassOverviewHTML((ClassExecutionData) pData, pClassName);
            if (content != null) {
                writer.write(content);
            }
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    private static String createClassOverviewHTML(ClassExecutionData pData, String pClassFileName) {
        HTMLFile html = new HTMLFile(pClassFileName);

        html.createHeader();

        List<String> columns = new ArrayList<>(Arrays.asList("Element", "Total", "Covered", "Missed"));
        html.addTable(columns, pData, pClassFileName);
        return html.render();
    }

    public static void createClassDetailView(String pClassName, ExecutionData pData, String pWorkDir, String sourceDir)
            throws IOException {
        if (pData instanceof ClassExecutionData) {
            System.out.println(new PrettyPrintMap<>(((ClassExecutionData) pData).getDefUsePairs()));
            String filePath = String.format("%s/%s.java.html", pWorkDir, pClassName);
            File detailView = new File(filePath);

            String fileName = String.format("%s/%s.java", sourceDir, ((ClassExecutionData) pData).getRelativePath());
            File classFile = new File(fileName);
            Scanner scanner = new Scanner(classFile);
            HTMLFile html = new HTMLFile(pClassName);

            // TODO: Create/Store style files and create links in header
            html.createHeader();
            int lineCounter = 0;
            Table table = new Table(null);
            while (scanner.hasNextLine()) {
                String current = scanner.nextLine();
                lineCounter += 1;
                // create line string with everything
                current = findAndMarkVariables(lineCounter, current, (ClassExecutionData) pData);
                Span span = new Span(current, lineCounter);
                String[] content = {String.valueOf(lineCounter), span.render()};
                table.addRow(new Row(content, null));
            }
            scanner.close();
            html.addTable(table);

            Writer writer = new FileWriter(detailView);
            writer.write(html.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    private static String findAndMarkVariables(int lineNumber, String lineString, ClassExecutionData data) {
        String[] specialChars = lineString.split("\\w+\\b");
        String[] words = lineString.split("\\W+");
        boolean haveEqualLength = words.length == specialChars.length;
        StringBuilder builder = new StringBuilder();
        if (words.length == 0) {
            for (String spChar : specialChars) {
                spChar = spChar.replace(" ", "&nbsp;");
                builder.append(spChar);
            }
        } else {
            if (!haveEqualLength) {
                builder.append(specialChars[0]);
            }
            for (int i = 0; i < words.length; i++) {
                // TODO: mark pairs
                String str = words[i];
                ProgramVariable definition = findDefinition(data, lineNumber, str);
                if (definition != null) {
                    Map<ProgramVariable, Boolean> uses = findUses(data, definition);
                    // Create dropdown and links to variables
                    words[i] = createDropDown(uses, str);
                } else {
                    // Mark uses appropriately
                    if (isCovered(data, lineNumber, str)) {
                        words[i] = String.format("<span style=\"background-color:#7EFF8D\">%s</span>", str);
                    }

                    if (isUncovered(data, lineNumber, str)) {
                        words[i] = String.format("<span style=\"background-color:#FF7E7E\">%s</span>", str);
                    }
                }
                if (haveEqualLength) {
                    specialChars[i] = specialChars[i].replace(" ", "&nbsp;");
                    builder.append(words[i]).append(specialChars[i]);
                } else {
                    specialChars[i + 1] = specialChars[i + 1].replace(" ", "&nbsp;");
                    builder.append(words[i]).append(specialChars[i + 1]);
                }
            }
        }
        return builder.toString();
    }

    private static String createDropDown(final Map<ProgramVariable, Boolean> pUses, String pString) {
        // create table and show colored entries on hover over
        // entries are links to the variable use
        String backgroundColor;
        if (Collections.frequency(pUses.values(), true) == pUses.size()) {
            backgroundColor = "#7EFF8D";
        } else if (Collections.frequency(pUses.values(), true) == 0) {
            // red
            backgroundColor = "#FF7E7E";
        } else {
            backgroundColor = "#FFE27E";
        }
        String table = String.format("<span class=\"tooltiptext\">%s</span>", "Tooltip");

        return String.format("<div class=\"tooltip\" style=\"background-color:%s\">%s%s</div>", backgroundColor, pString, table);
    }

    // find definition by line number and name
    private static ProgramVariable findDefinition(ClassExecutionData pData, int pLineNumber, String pName){
        for(Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for(DefUsePair defUsePair : defUsePairs.getValue()) {
                ProgramVariable definition = defUsePair.getDefinition();
                if (definition.getLineNumber() == pLineNumber && definition.getName().equals(pName)){
                    return definition;
                }
            }
        }
        return null;
    }

    // find all uses for one particular definition
    private static Map<ProgramVariable, Boolean> findUses(ClassExecutionData pData, ProgramVariable pDefinition) {
        Map<ProgramVariable, Boolean> uses = new HashMap<>();
        for(Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for(DefUsePair defUsePair : defUsePairs.getValue()) {
                if (defUsePair.getDefinition().equals(pDefinition)){
                    ProgramVariable use = defUsePair.getUsage();
                    boolean covered = pData.getDefUseUncovered().get(defUsePairs.getKey()).contains(use);
                    uses.put(use, covered);
                }
            }
        }
        return uses;
    }

    private static boolean isUncovered(ClassExecutionData data, int lineNumber, String name) {
        for (Map.Entry<String, Set<ProgramVariable>> map : data.getDefUseUncovered().entrySet()) {
            for (ProgramVariable var : map.getValue()) {
                if (var.getName().equals(name) && var.getLineNumber() == lineNumber) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCovered(ClassExecutionData data, int lineNumber, String name) {
        for (Map.Entry<String, Set<ProgramVariable>> map : data.getDefUseCovered().entrySet()) {
            for (ProgramVariable var : map.getValue()) {
                if (var.getName().equals(name) && var.getLineNumber() == lineNumber) {
                    return true;
                }
            }
        }
        return false;
    }
}
