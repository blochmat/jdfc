package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;

import java.io.*;
import java.util.*;


// TODO: pro/contra static methods
public class HTMLFactory {

    public static void generateIndexFiles(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                          String pWorkDir,
                                          boolean isRoot) throws IOException {
        String indexPath = String.format("%s/index.html", pWorkDir);
        File index = new File(indexPath);
        HTMLElement indexHTML = createIndexHTML(pClassFileDataMap, pWorkDir, isRoot);
        Writer writer = new FileWriter(index);
        writer.write(indexHTML.render());
        writer.close();
        if (!isRoot) {
            // TODO: Create indexSource files?
            String indexSourcePath = String.format("%s/index.source.html", pWorkDir);
            File indexSource = new File(indexSourcePath);
            indexSource.createNewFile();
        }
    }

    public static void createClassOverview(String pClassName, ExecutionData pData, String pWorkDir, boolean isRootDir) throws IOException {
        if (pData instanceof ClassExecutionData) {
            String filePath = String.format("%s/%s.html", pWorkDir, pClassName);
            File classFile = new File(filePath);
            HTMLElement classHTML = createClassOverviewHTML((ClassExecutionData) pData, pClassName, isRootDir);
            Writer writer = new FileWriter(classFile);
            writer.write(classHTML.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    public static void createClassDetailView(String pClassName, ExecutionData pData, String pWorkDir, String sourceDir)
            throws IOException {
        if (pData instanceof ClassExecutionData) {
            String outPath = String.format("%s/%s.java.html", pWorkDir, pClassName);
            File outFile = new File(outPath);

            String inPath = String.format("%s/%s.java", sourceDir, ((ClassExecutionData) pData).getRelativePath());
            File inFile = new File(inPath);
            Scanner scanner = new Scanner(inFile);
            HTMLElement htmlMainTag = HTMLElement.html(null);
            String classFileName = String.format("%s.java", pClassName);

            htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, false));
            HTMLElement bodyTag = HTMLElement.body(null);
            bodyTag.getContent().add(HTMLElement.h1(null, pClassName));
            HTMLElement tableTag = HTMLElement.table(null);
            // TODO: Create/Store style files and create links in header
            int lineCounter = 0;
            while (scanner.hasNextLine()) {
                HTMLElement trTag = HTMLElement.tr(null);
                String current = scanner.nextLine();
                lineCounter += 1;
                trTag.getContent().add(HTMLElement.td(null, lineCounter));
                HTMLElement tdTag = HTMLElement.td(null);
                tdTag.getContent().add(createPreTag(lineCounter, current, (ClassExecutionData) pData));
                trTag.getContent().add(tdTag);
                tableTag.getContent().add(trTag);
            }
            scanner.close();
            bodyTag.getContent().add(tableTag);
            htmlMainTag.getContent().add(bodyTag);
            Writer writer = new FileWriter(outFile);
            writer.write(htmlMainTag.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    private static HTMLElement createIndexHTML(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                               final String pWorkDir,
                                               final boolean isRootDir) {
        String[] split = pWorkDir.split("/");
        String title = split[split.length - 1];
        HTMLElement htmlMainTag = HTMLElement.html(null);
        htmlMainTag.getContent().add(createDefaultHTMLHead(title, isRootDir));
        HTMLElement htmlBodyTag = HTMLElement.body(null);
        htmlBodyTag.getContent().add(HTMLElement.h1(null, title));
        List<String> columns = new ArrayList<>(Arrays.asList("Method Count", "Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pClassFileDataMap));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private static HTMLElement createClassOverviewHTML(ClassExecutionData pData, String pClassFileName, boolean isRootDir) {
        HTMLElement htmlMainTag = HTMLElement.html(null);
        String classFileName = String.format("%s.java", pClassFileName);
        htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, isRootDir));
        HTMLElement htmlBodyTag = HTMLElement.body(null);
        htmlBodyTag.getContent().add(HTMLElement.h1(null, pClassFileName));
        List<String> columns = new ArrayList<>(Arrays.asList("Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pData, pClassFileName));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private static HTMLElement createPreTag(int lineNumber, String lineString, ClassExecutionData data) {
        HTMLElement spanTagLine = HTMLElement.span(null);
        String[] specialChars = lineString.split("\\w+\\b");
        String[] words = lineString.split("\\W+");
        boolean haveEqualLength = words.length == specialChars.length;

        if (words.length == 0) {
            StringBuilder builder = new StringBuilder();
            for(String c : specialChars) {
                builder.append(c);
            }
            spanTagLine.getContent().add(HTMLElement.noTag(builder.toString()));
        } else {
            if (!haveEqualLength) {
                spanTagLine.getContent().add(HTMLElement.noTag(specialChars[0]));
            }
            for (int i = 0; i < words.length; i++) {
                // TODO: mark pairs
                String word = words[i];
                ProgramVariable definition = findDefinition(data, lineNumber, word);
                HTMLElement spanTag;
                if (definition != null) {
                    Map<ProgramVariable, Boolean> uses = findUses(data, definition);
                    String styleClass =
                            String.format("class=\"%s\" ", getDefinitionBackgroundColorHex(uses));
                    spanTag = HTMLElement.span(styleClass);
                    // Create dropdown and links to variables
                    spanTag.getContent().add(createTooltip(uses, word));
                } else {
                    spanTag = HTMLElement.span(null, word);
                    // TODO: Redefinitions are marked green
                    if (isCovered(data, lineNumber, word)) {
                        spanTag.getAttributes().add("class=\"green\" ");
                    }
                    if (isUncovered(data, lineNumber, word)) {
                        spanTag.getAttributes().add("class=\"red\" ");
                    }
                }
                spanTagLine.getContent().add(spanTag);
                if (haveEqualLength) {
//                    specialChars[i] = specialChars[i].replace(" ", "&nbsp;");
//                    builder.append(words[i]).append(specialChars[i]);
                    spanTagLine.getContent().add(HTMLElement.noTag(specialChars[i]));
                } else {
//                    specialChars[i + 1] = specialChars[i + 1].replace(" ", "&nbsp;");
//                    builder.append(words[i]).append(specialChars[i + 1]);
                    spanTagLine.getContent().add(HTMLElement.noTag(specialChars[i + 1]));
                }
            }
        }
        return spanTagLine;
    }

    private static HTMLElement createDefaultHTMLHead(final String pTitle, boolean isRootDir) {
        HTMLElement headTag = HTMLElement.head(null);
        String href;
        if(isRootDir){
            href = "../jdfc-resources/report.css";
        } else {
            href = "../../jdfc-resources/report.css";
        }
        headTag.getContent().add(
                HTMLElement.link("stylesheet", href, "text/css"));
        headTag.getContent().add(
                HTMLElement.title(null, pTitle));
        return headTag;
    }

    private static HTMLElement createDataTable(final List<String> pColumns, Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement tableTag = HTMLElement.table(null);
        tableTag.getContent().add(createTableHeadTag(pColumns));
        tableTag.getContent().add(createTableBodyTag(pClassFileDataMap));
        tableTag.getContent().add(createTableFootTag(pClassFileDataMap));
        return tableTag;
    }

    private static HTMLElement createDataTable(final List<String> pColumns,
                                               final ClassExecutionData pData,
                                               final String pClassfileName) {
        HTMLElement tableTag = HTMLElement.table(null);
        tableTag.getContent().add(createTableHeadTag(pColumns));
        for (Map.Entry<String, List<DefUsePair>> entry : pData.getDefUsePairs().entrySet()) {
            int total = entry.getValue().size();
            int covered = pData.computeCoverageForMethod(entry.getKey());
            int missed = total - covered;
            String link = String.format("%s.java.html#L%s", pClassfileName, pData.getMethodStartLineMap().get(entry.getKey()));

            HTMLElement trTag = HTMLElement.tr(null);
            HTMLElement tdTag = HTMLElement.td(null);
            tdTag.getContent().add(HTMLElement.a(null, link, entry.getKey()));
            trTag.getContent().add(tdTag);
            trTag.getContent().add(HTMLElement.td(null, total));
            trTag.getContent().add(HTMLElement.td(null, covered));
            trTag.getContent().add(HTMLElement.td(null, missed));
            tableTag.getContent().add(trTag);
        }
        tableTag.getContent().add(createTableFootTag(pData));
        return tableTag;
    }

    private static HTMLElement createTableHeadTag(final List<String> pColumns) {
        HTMLElement theadTag = HTMLElement.thead(null);
        theadTag.getContent().add(HTMLElement.td(null, "Element"));
        for (String col : pColumns) {
            theadTag.getContent().add(HTMLElement.td(null, col));
        }
        return theadTag;
    }

    private static HTMLElement createTableFootTag(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement tfootTag = HTMLElement.tfoot(null);
        Map.Entry<String, ExecutionDataNode<ExecutionData>> entry = pClassFileDataMap.entrySet().iterator().next();
        ExecutionData parentData = entry.getValue().getParent().getData();
        tfootTag.getContent().add(HTMLElement.td(null, "Total"));
        tfootTag.getContent().add(HTMLElement.td(null, parentData.getMethodCount()));
        tfootTag.getContent().add(HTMLElement.td(null, parentData.getTotal()));
        tfootTag.getContent().add(HTMLElement.td(null, parentData.getCovered()));
        tfootTag.getContent().add(HTMLElement.td(null, parentData.getMissed()));
        return tfootTag;
    }

    private static HTMLElement createTableFootTag(final ClassExecutionData pData) {
        HTMLElement tfootTag = HTMLElement.tfoot(null);
        tfootTag.getContent().add(HTMLElement.td(null, "Total"));
        tfootTag.getContent().add(HTMLElement.td(null, pData.getTotal()));
        tfootTag.getContent().add(HTMLElement.td(null, pData.getCovered()));
        tfootTag.getContent().add(HTMLElement.td(null, pData.getMissed()));
        return tfootTag;
    }

    private static HTMLElement createTableBodyTag(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement bodyTag = HTMLElement.body(null);
        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : pClassFileDataMap.entrySet()) {
            ExecutionData data = entry.getValue().getData();
            HTMLElement trTag = HTMLElement.tr(null);

            // First link tag
            HTMLElement tdTag = HTMLElement.td(null);
            if (data instanceof ClassExecutionData) {
                String link = String.format("%s.html", entry.getKey());
                tdTag.getContent().add(HTMLElement.a(null, link, entry.getKey()));
            } else {
                tdTag.getContent().add(HTMLElement.a(null, entry.getKey(), entry.getKey()));
            }
            trTag.getContent().add(tdTag);
            trTag.getContent().add(HTMLElement.td(null, data.getMethodCount()));
            trTag.getContent().add(HTMLElement.td(null, data.getTotal()));
            trTag.getContent().add(HTMLElement.td(null, data.getCovered()));
            trTag.getContent().add(HTMLElement.td(null, data.getMissed()));
            bodyTag.getContent().add(trTag);
        }
        return bodyTag;
    }

    private static HTMLElement createTooltip(final Map<ProgramVariable, Boolean> pUses, final String pText) {
        // create table and show colored entries on hover over
        // entries are links to the variable use
        String tooltipStyle = "class=\"tooltip\" ";
        HTMLElement tooltip = HTMLElement.div(tooltipStyle);
        String tooltipTextStyle = "class=\"tooltiptext\"";
//        HTMLElement divTag = HTMLElement.div(tooltipTextStyle);
//        HTMLElement tableTag = HTMLElement.table(null);
//        HTMLElement tbody = HTMLElement.tbody(null);
//        HTMLElement tr = HTMLElement.tr(null);
//        HTMLElement td = HTMLElement.td(null);
//        td.getContent().add(HTMLElement.a("Link it is", null, "EvenMoreBranchingInteger.java.html#L3"));
//        tr.getContent().add(td);
//        tbody.getContent().add(tr);
//        tableTag.getContent().add(tbody);
        tooltip.getContent().add(HTMLElement.noTag(pText));
        tooltip.getContent().add(HTMLElement.a(tooltipTextStyle, "EvenMoreBranchingInteger.java.html#L3", "A Link it is"));
//        tooltip.getContent().add(divTag);
        return tooltip;
    }

    private static String getDefinitionBackgroundColorHex(Map<ProgramVariable, Boolean> pUses) {
        if (Collections.frequency(pUses.values(), true) == pUses.size()) {
            return "green";
        } else if (Collections.frequency(pUses.values(), true) == 0) {
            return "red";
        } else {
            return "yellow";
        }
    }

    // find definition by line number and name
    private static ProgramVariable findDefinition(ClassExecutionData pData, int pLineNumber, String pName) {
        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for (DefUsePair defUsePair : defUsePairs.getValue()) {
                ProgramVariable definition = defUsePair.getDefinition();
                if (definition.getLineNumber() == pLineNumber && definition.getName().equals(pName)) {
                    return definition;
                }
            }
        }
        return null;
    }

    // find all uses for one particular definition
    private static Map<ProgramVariable, Boolean> findUses(ClassExecutionData pData, ProgramVariable pDefinition) {
        Map<ProgramVariable, Boolean> uses = new HashMap<>();
        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for (DefUsePair defUsePair : defUsePairs.getValue()) {
                if (defUsePair.getDefinition().equals(pDefinition)) {
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
