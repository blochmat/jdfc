package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.InstanceVariable;
import com.jdfc.core.analysis.cfg.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.report.html.resources.Resources;

import java.io.*;
import java.util.*;

public class HTMLFactory {

    static final String[] JAVA_KEYWORDS = {"abstract", "assert",
            "break", "case", "catch", "class", "const",
            "continue", "default", "do", "else", "extends", "false",
            "final", "finally", "for", "goto", "if", "implements",
            "import", "instanceof", "interface", "native",
            "new", "null", "package", "private", "protected", "public",
            "return", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while"};

    static final String[] TYPE_KEYWORDS = {"boolean", "byte", "char", "double", "float", "int", "long", "short"};

    static final String STYLE_SHEET = "report.css";

    static final String SCRIPT = "script.js";

    public static void generateIndexFiles(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                          final String pWorkDir,
                                          final Resources pResources) throws IOException {
        String indexPath = String.format("%s/index.html", pWorkDir);
        File index = new File(indexPath);
        String styleSheetPath = String.format("%s/%s", pResources.getPathToResourcesFrom(index), STYLE_SHEET);
        String scriptPath = String.format("%s/%s", pResources.getPathToResourcesFrom(index), SCRIPT);
        HTMLElement indexHTML = createIndexHTML(pClassFileDataMap, pWorkDir, styleSheetPath, scriptPath);
        Writer writer = new FileWriter(index);
        writer.write(indexHTML.render());
        writer.close();
        // if is not root
//        if (!pOut) {
//            // TODO: Create indexSource files?
//            String indexSourcePath = String.format("%s/index.source.html", pWorkDir);
//            File indexSource = new File(indexSourcePath);
//            indexSource.createNewFile();
//        }
    }

    public static void createClassOverview(final String pClassName,
                                           final ExecutionData pData,
                                           final String pWorkDir,
                                           final Resources pResources) throws IOException {
        if (pData instanceof ClassExecutionData) {
            String filePath = String.format("%s/%s.html", pWorkDir, pClassName);
            File classFile = new File(filePath);
            String styleSheetPath = String.format("%s/%s", pResources.getPathToResourcesFrom(classFile), STYLE_SHEET);
            String scriptPath = String.format("%s/%s", pResources.getPathToResourcesFrom(classFile), SCRIPT);
            HTMLElement classHTML = createClassOverviewHTML((ClassExecutionData) pData, pClassName, styleSheetPath, scriptPath);
            Writer writer = new FileWriter(classFile);
            writer.write(classHTML.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    public static void createClassDetailView(final String pClassName,
                                             final ExecutionData pData,
                                             final String pWorkDir,
                                             final String pSourceDir,
                                             final Resources pResources)
            throws IOException {
        if (pData instanceof ClassExecutionData) {
            String outPath = String.format("%s/%s.java.html", pWorkDir, pClassName);
            File classFile = new File(outPath);
            String styleSheetPath = String.format("%s/%s", pResources.getPathToResourcesFrom(classFile), STYLE_SHEET);
            String scriptPath = String.format("%s/%s", pResources.getPathToResourcesFrom(classFile), SCRIPT);
            String inPath = String.format("%s/%s.java", pSourceDir, ((ClassExecutionData) pData).getRelativePath());
            File inFile = new File(inPath);
            Scanner scanner = new Scanner(inFile);
            HTMLElement htmlMainTag = HTMLElement.html();
            String classFileName = String.format("%s.java", pClassName);

            htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, styleSheetPath));
            HTMLElement bodyTag = HTMLElement.body();
            bodyTag.getAttributes().add("class=\"style-class\"");
            bodyTag.getContent().add(HTMLElement.script("text/javascript", scriptPath));
            bodyTag.getAttributes().add("onload=\"sortTables()\"");
            bodyTag.getContent().add(HTMLElement.h1(pClassName));
            HTMLElement tableTag = HTMLElement.table();
            tableTag.getAttributes().add("id=\"classDetailView\"");
            // TODO: Create/Store style files and create links in header
            int lineCounter = 0;
            while (scanner.hasNextLine()) {
                HTMLElement trTag = HTMLElement.tr();
                String current = scanner.nextLine();
                lineCounter += 1;
                HTMLElement lineCell = HTMLElement.td(lineCounter);
                lineCell.getAttributes().add(String.format("id=\"%s\"", lineCounter));
                trTag.getContent().add(lineCell);
                HTMLElement textCell = HTMLElement.td();
                textCell.getContent().add(processLine(pClassName, lineCounter, current, (ClassExecutionData) pData));
                trTag.getContent().add(textCell);
                tableTag.getContent().add(trTag);
            }
            scanner.close();
            bodyTag.getContent().add(tableTag);
            htmlMainTag.getContent().add(bodyTag);
            Writer writer = new FileWriter(classFile);
            writer.write(htmlMainTag.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    private static HTMLElement createIndexHTML(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                               final String pWorkDir,
                                               final String pPathToStyleSheet,
                                               final String pPathToScript) {
        String[] split = pWorkDir.split("/");
        String title = split[split.length - 1];
        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(title, pPathToStyleSheet));
        HTMLElement htmlBodyTag = HTMLElement.body();
        htmlBodyTag.getContent().add(HTMLElement.script("text/javascript", pPathToScript));
        htmlBodyTag.getAttributes().add("onload=\"sortTables()\"");
        htmlBodyTag.getContent().add(HTMLElement.h1(title));
        List<String> columns = new ArrayList<>(Arrays.asList("Method Count", "Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pClassFileDataMap));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private static HTMLElement createClassOverviewHTML(final ClassExecutionData pData,
                                                       final String pClassFileName,
                                                       final String pPathToStyleSheet,
                                                       final String pPathToScript) {
        HTMLElement htmlMainTag = HTMLElement.html();
        String classFileName = String.format("%s.java", pClassFileName);
        htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, pPathToStyleSheet));
        HTMLElement htmlBodyTag = HTMLElement.body();
        htmlBodyTag.getAttributes().add("onload=\"sortTables()\"");
        htmlBodyTag.getContent().add(HTMLElement.script("text/javascript", pPathToScript));
        htmlBodyTag.getContent().add(HTMLElement.h1(pClassFileName));
        List<String> columns = new ArrayList<>(Arrays.asList("Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pData, pClassFileName));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private static HTMLElement processLine(String pClassName, int lineNumber, String lineString, ClassExecutionData data) {
        HTMLElement spanTagLine = HTMLElement.span();
        spanTagLine.getAttributes().add("class=\"keep-spaces\"");
        String[] specialCharsArray = lineString.split("\\w+\\b");
        String[] wordsArray = lineString.split("\\W+");

        List<String> specialChars = new ArrayList<>();
        for (String str : specialCharsArray) {
            if (!str.equals("")) {
                specialChars.add(str);
            }
        }
        List<String> words = new ArrayList<>();
        for (String str : wordsArray) {
            if (!str.equals("")) {
                words.add(str);
            }
        }

        boolean isSpecialCharsLonger = specialChars.size() > words.size();
        boolean isComment = false;

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            // Create Comment
            if (specialChars.get(i).contains("//")) {
                int index = specialChars.get(i).indexOf("//");
                String remainder = specialChars.get(i).substring(0, index);
                String commentSlashes = specialChars.get(i).substring(index);
                spanTagLine.getContent().add(HTMLElement.noTag(remainder));
                HTMLElement comment = HTMLElement.span();
                comment.getAttributes().add("class=\"comment\"");
                comment.getContent().add(HTMLElement.noTag(commentSlashes));
                comment.getContent().add(HTMLElement.noTag(words.get(i)));
                for (int j = i + 1; j < words.size(); j++) {
                    comment.getContent().add(HTMLElement.noTag(specialChars.get(j)));
                    comment.getContent().add(HTMLElement.noTag(words.get(j)));
                }
                if (isSpecialCharsLonger) {
                    comment.getContent().add(HTMLElement.noTag(specialChars.get(specialChars.size() - 1)));
                    isComment = true;
                }
                spanTagLine.getContent().add(comment);
                break;
            }

            ProgramVariable definition = findDefinition(data, lineNumber, word);
            HTMLElement spanTag;
            if (definition != null) {
                boolean isDefCovered = findIsDefCovered(data, definition);
                Map<ProgramVariable, Boolean> useCoverageMap = getUseCoverageMap(data, definition);
                spanTag = HTMLElement.span();
                String id = String.format("L%sI%s", definition.getLineNumber(), definition.getInstructionIndex());
                spanTag.getAttributes().add(String.format("id=\"%s\"", id));
                spanTag.getAttributes().add(String.format("class=\"%s\"", getDefinitionBackgroundColorHex(isDefCovered, useCoverageMap)));
                spanTag.getContent().add(createTooltip(pClassName, useCoverageMap, word));
            } else {
                ProgramVariable usage = findUsage(data, lineNumber, word);
                spanTag = HTMLElement.span(word);
                if (usage != null) {
                    String id = String.format("L%sI%s", usage.getLineNumber(), usage.getInstructionIndex());
                    spanTag.getAttributes().add(String.format("id=\"%s\"", id));
                    if (isCovered(data, usage)) {
                        spanTag.getAttributes().add("class=\"green\"");
                    }
                    if (isUncovered(data, usage)) {
                        spanTag.getAttributes().add("class=\"red\"");
                    }
                } else {
                    if(isRedefined(data, lineNumber, word)) {
                        spanTag = HTMLElement.span(word);
                        spanTag.getAttributes().add("class=\"blue\"");
//                    spanTag.getAttributes().add(String.format("class=\"%s\"", getRedefinitionBackgroundColorHex(data, lineNumber, word)));
                    }
//                    addCodeHighlighting(spanTag, word);
                }
            }
            if (isSpecialCharsLonger) {
                spanTagLine.getContent().add(HTMLElement.noTag(specialChars.get(i)));
                spanTagLine.getContent().add(spanTag);
            } else {
                spanTagLine.getContent().add(spanTag);
                spanTagLine.getContent().add(HTMLElement.noTag(specialChars.get(i)));
            }
        }
        // Add last special char
        if (isSpecialCharsLonger && !isComment) {
            spanTagLine.getContent().add(HTMLElement.noTag(specialChars.get(specialChars.size() - 1)));
        }

        return spanTagLine;
    }

    private static String getRedefinitionBackgroundColorHex(ClassExecutionData pData, int pLineNumber, String pName) {
        boolean covered = false;
        boolean uncovered = false;
        for (Map.Entry<String, Set<ProgramVariable>> defUseCovered : pData.getDefUseCovered().entrySet()) {
            for (ProgramVariable programVariable : defUseCovered.getValue()) {
                if (programVariable.getLineNumber() > pLineNumber
                        && programVariable.getName().equals(pName)
                        && pData.getMethodRangeMap().get(defUseCovered.getKey()).fst < pLineNumber
                        && pData.getMethodRangeMap().get(defUseCovered.getKey()).snd > pLineNumber) {
                    covered = true;
                }
            }
        }
        for (Map.Entry<String, Set<ProgramVariable>> defUseUncovered : pData.getDefUseUncovered().entrySet()) {
            for (ProgramVariable programVariable : defUseUncovered.getValue()) {
                if (programVariable.getLineNumber() > pLineNumber
                        && programVariable.getName().equals(pName)
                        && pData.getMethodRangeMap().get(defUseUncovered.getKey()).fst < pLineNumber
                        && pData.getMethodRangeMap().get(defUseUncovered.getKey()).snd > pLineNumber) {
                    uncovered = true;
                }
            }
        }

        if(uncovered && !covered) {
            return "red";
        } else if (covered && !uncovered) {
            return "blue";
        } else if (covered) {
            return "pink";
        } else {
            return "";
        }
    }

    private static HTMLElement createDefaultHTMLHead(final String pTitle,
                                                     final String pPathToResources) {
        HTMLElement headTag = HTMLElement.head();
        headTag.getContent().add(
                HTMLElement.link("stylesheet", pPathToResources, "text/css"));
        headTag.getContent().add(
                HTMLElement.title(pTitle));
        return headTag;
    }

    private static HTMLElement createDataTable(final List<String> pColumns, Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getContent().add(createTableHeadTag(pColumns));
        tableTag.getContent().add(createTableBodyTag(pClassFileDataMap));
        tableTag.getContent().add(createTableFootTag(pClassFileDataMap));
        return tableTag;
    }

    private static HTMLElement createDataTable(final List<String> pColumns,
                                               final ClassExecutionData pData,
                                               final String pClassfileName) {
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getContent().add(createTableHeadTag(pColumns));
        for (Map.Entry<String, List<DefUsePair>> entry : pData.getDefUsePairs().entrySet()) {
            String elementName = entry.getKey();
            if (elementName.contains("<init>")) {
                elementName = elementName.replace("<init>", "init");
            }
            int total = entry.getValue().size();
            int covered = pData.computeCoverageForMethod(entry.getKey());
            int missed = total - covered;
            String link = String.format("%s.java.html#L%s", pClassfileName,
                    pData.getMethodRangeMap().get(entry.getKey()).fst);
            HTMLElement trTag = HTMLElement.tr();
            HTMLElement tdTag = HTMLElement.td();
            tdTag.getContent().add(HTMLElement.a(link, elementName));
            trTag.getContent().add(tdTag);
            trTag.getContent().add(HTMLElement.td(total));
            trTag.getContent().add(HTMLElement.td(covered));
            trTag.getContent().add(HTMLElement.td(missed));
            tableTag.getContent().add(trTag);
        }
        tableTag.getContent().add(createTableFootTag(pData));
        return tableTag;
    }

    private static HTMLElement createTableHeadTag(final List<String> pColumns) {
        HTMLElement theadTag = HTMLElement.thead();
        theadTag.getContent().add(HTMLElement.td("Element"));
        for (String col : pColumns) {
            theadTag.getContent().add(HTMLElement.td(col));
        }
        return theadTag;
    }

    private static HTMLElement createTableFootTag(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement tfootTag = HTMLElement.tfoot();
        Map.Entry<String, ExecutionDataNode<ExecutionData>> entry = pClassFileDataMap.entrySet().iterator().next();
        ExecutionData parentData = entry.getValue().getParent().getData();
        tfootTag.getContent().add(HTMLElement.td("Total"));
        tfootTag.getContent().add(HTMLElement.td(parentData.getMethodCount()));
        tfootTag.getContent().add(HTMLElement.td(parentData.getTotal()));
        tfootTag.getContent().add(HTMLElement.td(parentData.getCovered()));
        tfootTag.getContent().add(HTMLElement.td(parentData.getMissed()));
        return tfootTag;
    }

    private static HTMLElement createTableFootTag(final ClassExecutionData pData) {
        HTMLElement tfootTag = HTMLElement.tfoot();
        tfootTag.getContent().add(HTMLElement.td("Total"));
        tfootTag.getContent().add(HTMLElement.td(pData.getTotal()));
        tfootTag.getContent().add(HTMLElement.td(pData.getCovered()));
        tfootTag.getContent().add(HTMLElement.td(pData.getMissed()));
        return tfootTag;
    }

    private static HTMLElement createTableBodyTag(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement bodyTag = HTMLElement.body();
        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : pClassFileDataMap.entrySet()) {
            ExecutionData data = entry.getValue().getData();
            HTMLElement trTag = HTMLElement.tr();

            // First link tag
            HTMLElement tdTag = HTMLElement.td();
            if (data instanceof ClassExecutionData) {
                String link = String.format("%s.html", entry.getKey());
                tdTag.getContent().add(HTMLElement.a(link, entry.getKey()));
            } else {
                tdTag.getContent().add(HTMLElement.a(entry.getKey(), entry.getKey()));
            }
            trTag.getContent().add(tdTag);
            trTag.getContent().add(HTMLElement.td(data.getMethodCount()));
            trTag.getContent().add(HTMLElement.td(data.getTotal()));
            trTag.getContent().add(HTMLElement.td(data.getCovered()));
            trTag.getContent().add(HTMLElement.td(data.getMissed()));
            bodyTag.getContent().add(trTag);
        }
        return bodyTag;
    }

    private static HTMLElement createTooltip(final String pClassName, final Map<ProgramVariable, Boolean> pUses, final String pText) {
        // create table and show colored entries on hover over
        // entries are links to the variable use
        HTMLElement tooltip = HTMLElement.div();
        tooltip.getAttributes().add("class=\"tooltip\"");
        tooltip.getContent().add(HTMLElement.span(pText));
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getAttributes().add("class=\"tooltipcontent\"");
        tooltip.getContent().add(tableTag);
        HTMLElement tbody = HTMLElement.tbody();
        tableTag.getContent().add(tbody);
        if (pUses.size() == 0) {
            HTMLElement tr = HTMLElement.tr();
            tbody.getContent().add(tr);
            HTMLElement td = HTMLElement.td("No use found.");
            tr.getContent().add(td);
            td.getAttributes().add("class=\"red\"");
        } else {
            for (Map.Entry<ProgramVariable, Boolean> use : pUses.entrySet()) {
                ProgramVariable variable = use.getKey();
                int lineNumber = variable.getLineNumber();
                HTMLElement tr = HTMLElement.tr();
                tbody.getContent().add(tr);
                tr.getContent().add(HTMLElement.td(lineNumber));
                HTMLElement td = HTMLElement.td();
                tr.getContent().add(td);
                String link = String.format("%s.java.html#L%sI%s", pClassName, variable.getLineNumber(), variable.getInstructionIndex());
                td.getContent().add(HTMLElement.a(link, variable.getName()));
                if (use.getValue()) {
                    td.getAttributes().add("class=\"green\"");
                } else {
                    td.getAttributes().add("class=\"red\"");
                }
            }
        }
        return tooltip;
    }

    private static void addCodeHighlighting(HTMLElement pSpanTag, String pWord) {
        if (isJavaKeyword(pWord)) {
            pSpanTag.getAttributes().add("class=\"java-keyword\"");
        } else if (isTypeKeyword(pWord)) {
            pSpanTag.getAttributes().add("class=\"type-keyword\"");
        }
    }

    private static boolean isJavaKeyword(String pWord) {
        return (Arrays.binarySearch(JAVA_KEYWORDS, pWord) >= 0);
    }

    private static boolean isTypeKeyword(String pWord) {
        return (Arrays.binarySearch(TYPE_KEYWORDS, pWord) >= 0);
    }

    private static String getDefinitionBackgroundColorHex(boolean pDefCovered, Map<ProgramVariable, Boolean> pUses) {
        if (!pDefCovered || pUses.size() == 0 || Collections.frequency(pUses.values(), true) == 0) {
            return "red";
        } else if (Collections.frequency(pUses.values(), true) == pUses.size()) {
            return "green";
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
        for (InstanceVariable instanceVariable : pData.getInstanceVariables()) {
            if (instanceVariable.getLineNumber() == pLineNumber && instanceVariable.getName().equals(pName)) {
                return ProgramVariable.create(
                        instanceVariable.getOwner(),
                        instanceVariable.getName(),
                        instanceVariable.getDescriptor(),
                        Integer.MIN_VALUE,
                        instanceVariable.getLineNumber());
            }
        }
        return null;
    }

    private static boolean isRedefined(ClassExecutionData pData, int pLineNumber, String pName) {
        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for (DefUsePair defUsePair : defUsePairs.getValue()) {
                ProgramVariable definition = defUsePair.getDefinition();
                if (definition.getLineNumber() > pLineNumber
                        && definition.getName().equals(pName)
                        && pData.getMethodRangeMap().get(defUsePairs.getKey()).fst <= pLineNumber
                        && pData.getMethodRangeMap().get(defUsePairs.getKey()).snd >= pLineNumber) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ProgramVariable findUsage(ClassExecutionData pData, int pLineNumber, String pName) {
        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for (DefUsePair defUsePair : defUsePairs.getValue()) {
                ProgramVariable usage = defUsePair.getUsage();
                if (usage.getLineNumber() == pLineNumber && usage.getName().equals(pName)) {
                    return usage;
                }
            }
        }
        return null;
    }

    private static boolean findIsDefCovered(ClassExecutionData pData, ProgramVariable pDefinition) {
        for (Map.Entry<String, Set<ProgramVariable>> defUseCovered : pData.getDefUseCovered().entrySet()) {
            if (defUseCovered.getValue().contains(pDefinition)) {
                return true;
            }
        }
        return false;
    }

    // find all uses for one particular definition
    private static Map<ProgramVariable, Boolean> getUseCoverageMap(ClassExecutionData pData, ProgramVariable pDefinition) {
        Map<ProgramVariable, Boolean> uses = new HashMap<>();
        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for (DefUsePair defUsePair : defUsePairs.getValue()) {
                if (defUsePair.getDefinition().equals(pDefinition)) {
                    ProgramVariable use = defUsePair.getUsage();
                    boolean covered = pData.getDefUseCovered().get(defUsePairs.getKey()).contains(use);
                    uses.put(use, covered);
                }
            }
        }
        return uses;
    }

    private static boolean isUncovered(ClassExecutionData data, ProgramVariable pUsage) {
        for (Map.Entry<String, Set<ProgramVariable>> map : data.getDefUseUncovered().entrySet()) {
            if (map.getValue().contains(pUsage)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCovered(ClassExecutionData pData, ProgramVariable pUsage) {
        for (Map.Entry<String, Set<ProgramVariable>> map : pData.getDefUseCovered().entrySet()) {
            if (map.getValue().contains(pUsage)) {
                return true;
            }
        }
        return false;
    }
}
