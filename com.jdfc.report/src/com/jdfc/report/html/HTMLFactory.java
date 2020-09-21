package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.InstanceVariable;
import com.jdfc.core.analysis.cfg.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;

import java.io.*;
import java.util.*;


// TODO: pro/contra static methods
public class HTMLFactory {

    static final String[] JAVA_KEYWORDS = { "abstract", "assert",
            "break", "case", "catch", "class", "const",
            "continue", "default", "do", "else", "extends", "false",
            "final", "finally", "for", "goto", "if", "implements",
            "import", "instanceof", "interface", "native",
            "new", "null", "package", "private", "protected", "public",
            "return", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while" };

    static final String[] TYPE_KEYWORDS = {"boolean", "byte", "char", "double", "float", "int", "long", "short"};

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
            HTMLElement htmlMainTag = HTMLElement.html();
            String classFileName = String.format("%s.java", pClassName);

            htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, false));
            HTMLElement bodyTag = HTMLElement.body();
            bodyTag.getAttributes().add("class=\"style-class\"");
            bodyTag.getContent().add(HTMLElement.h1(pClassName));
            HTMLElement tableTag = HTMLElement.table();
            // TODO: Create/Store style files and create links in header
            int lineCounter = 0;
            while (scanner.hasNextLine()) {
                HTMLElement trTag = HTMLElement.tr();
                String current = scanner.nextLine();
                lineCounter += 1;
                trTag.getContent().add(HTMLElement.td(lineCounter));
                HTMLElement tdTag = HTMLElement.td();
                tdTag.getContent().add(processLine(pClassName, lineCounter, current, (ClassExecutionData) pData));
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
        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(title, isRootDir));
        HTMLElement htmlBodyTag = HTMLElement.body();
        htmlBodyTag.getContent().add(HTMLElement.h1(title));
        List<String> columns = new ArrayList<>(Arrays.asList("Method Count", "Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pClassFileDataMap));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private static HTMLElement createClassOverviewHTML(ClassExecutionData pData, String pClassFileName, boolean isRootDir) {
        HTMLElement htmlMainTag = HTMLElement.html();
        String classFileName = String.format("%s.java", pClassFileName);
        htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, isRootDir));
        HTMLElement htmlBodyTag = HTMLElement.body();
        htmlBodyTag.getContent().add(HTMLElement.h1(pClassFileName));
        List<String> columns = new ArrayList<>(Arrays.asList("Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pData, pClassFileName));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private static HTMLElement processLine(String pClassName, int lineNumber, String lineString, ClassExecutionData data) {
        HTMLElement spanTagLine = HTMLElement.span();
        spanTagLine.getAttributes().add("class=\"keep-spaces\"");
        String[] specialChars = lineString.split("\\w+\\b");
        String[] words = lineString.split("\\W+");
        boolean haveEqualLength = words.length == specialChars.length;

        if (words.length == 0) {
            StringBuilder builder = new StringBuilder();
            for (String c : specialChars) {
                builder.append(c);
            }
            spanTagLine.getContent().add(HTMLElement.noTag(builder.toString()));
        } else {
            if (!haveEqualLength) {
                spanTagLine.getContent().add(HTMLElement.noTag(specialChars[0]));
            }
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                ProgramVariable definition = findDefinition(data, lineNumber, word);
                HTMLElement spanTag;
                if (definition != null) {
                    Map<ProgramVariable, Boolean> useCoverageMap = getUseCoverageMap(data, definition);
                    spanTag = HTMLElement.span();
                    spanTag.getAttributes().add(String.format("class=\"%s\" ", getDefinitionBackgroundColorHex(useCoverageMap)));
                    spanTag.getContent().add(createTooltip(pClassName, useCoverageMap, word));
                } else {
                    ProgramVariable usage = findUsage(data, lineNumber, word);
                    spanTag = HTMLElement.span(word);
                    if (usage != null) {
                        String id = String.format("L%sI%s", usage.getLineNumber(), usage.getInstructionIndex());
                        spanTag.getAttributes().add(String.format("id=\"%s\"", id));
                        // TODO: Redefinitions are marked green
                        if (isCovered(data, usage)) {
                            spanTag.getAttributes().add("class=\"green\"");
                        }
                        if (isUncovered(data, usage)) {
                            spanTag.getAttributes().add("class=\"red\"");
                        }
                    } else {
                        addCodeHighlighting(spanTag, word);
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
        HTMLElement headTag = HTMLElement.head();
        String href;
        if (isRootDir) {
            href = "../jdfc-resources/report.css";
        } else {
            href = "../../jdfc-resources/report.css";
        }
        headTag.getContent().add(
                HTMLElement.link("stylesheet", href, "text/css"));
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
            int total = entry.getValue().size();
            int covered = pData.computeCoverageForMethod(entry.getKey());
            int missed = total - covered;
            String link = String.format("%s.java.html#L%s", pClassfileName, pData.getMethodStartLineMap().get(entry.getKey()));

            HTMLElement trTag = HTMLElement.tr();
            HTMLElement tdTag = HTMLElement.td();
            tdTag.getContent().add(HTMLElement.a(link, entry.getKey()));
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
        if(isJavaKeyword(pWord)){
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


    private static String getDefinitionBackgroundColorHex(Map<ProgramVariable, Boolean> pUses) {
        if (pUses.size() == 0 || Collections.frequency(pUses.values(), true) == 0) {
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
        for(InstanceVariable instanceVariable : pData.getInstanceVariables()) {
            if(instanceVariable.getLineNumber() == pLineNumber && instanceVariable.getName().equals(pName)){
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
