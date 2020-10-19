package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.ifg.DefUsePair;
import com.jdfc.core.analysis.ifg.InstanceVariable;
import com.jdfc.core.analysis.ifg.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.report.html.resources.Resources;

import java.io.*;
import java.util.*;

public class HTMLFactory {

    private final Resources resources;
    private final File baseDir;

    public HTMLFactory(Resources pResources, File pBaseDir) {
        baseDir = pBaseDir;
        resources = pResources;
        try {
            resources.copyResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final String[] JAVA_KEYWORDS = {"abstract", "assert",
            "break", "case", "catch", "class", "const",
            "continue", "default", "do", "else", "extends", "false",
            "final", "finally", "for", "goto", "if", "implements",
            "import", "instanceof", "interface", "native",
            "new", "null", "package", "private", "protected", "public",
            "return", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while"};

    final String[] TYPE_KEYWORDS = {"boolean", "byte", "char", "double", "float", "int", "long", "short"};

    final String STYLE_SHEET = "report.css";

    final String SCRIPT = "script.js";

    public void generateIndexFiles(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                   final File pWorkDir) throws IOException {
        String indexPath = String.format("%s/index.html", pWorkDir.toString());
        File index = new File(indexPath);
        String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(index), STYLE_SHEET);
        String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(index), SCRIPT);
        HTMLElement indexHTML = createIndexHTML(pClassFileDataMap, pWorkDir, styleSheetPath, scriptPath);
        Writer writer = new FileWriter(index);
        writer.write(indexHTML.render());
        writer.close();
    }

    public void createClassOverview(final String pClassName,
                                    final ExecutionData pData,
                                    final File pWorkDir) throws IOException {
        if (pData instanceof ClassExecutionData) {
            String filePath = String.format("%s/%s.html", pWorkDir.toString(), pClassName);
            File classFile = new File(filePath);
            String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(classFile), STYLE_SHEET);
            String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(classFile), SCRIPT);
            HTMLElement classHTML = createClassOverviewHTML((ClassExecutionData) pData, classFile, pClassName, styleSheetPath, scriptPath);
            Writer writer = new FileWriter(classFile);
            writer.write(classHTML.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from data.");
        }
    }

    public void createClassDetailView(final String pClassName,
                                      final ExecutionData pData,
                                      final File pWorkDir,
                                      final File pSourceDir)
            throws IOException {
        if (pData instanceof ClassExecutionData) {
            String outPath = String.format("%s/%s.java.html", pWorkDir.toString(), pClassName);
            File classFile = new File(outPath);
            String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(classFile), STYLE_SHEET);
            String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(classFile), SCRIPT);
            String inPath = String.format("%s/%s.java", pSourceDir.toString(), ((ClassExecutionData) pData).getRelativePath());
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

    private HTMLElement createIndexHTML(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                        final File pWorkDir,
                                        final String pPathToStyleSheet,
                                        final String pPathToScript) {
        String[] split = pWorkDir.toString().split("/");
        String title = split[split.length - 1];
        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(title, pPathToStyleSheet));
        HTMLElement htmlBodyTag = HTMLElement.body();
        htmlBodyTag.getContent().add(HTMLElement.script("text/javascript", pPathToScript));
        htmlBodyTag.getAttributes().add("onload=\"sortTables()\"");
        htmlBodyTag.getContent().add(createBreadcrumbs(pWorkDir));
        htmlBodyTag.getContent().add(HTMLElement.h1(title));
        List<String> columns = new ArrayList<>(Arrays.asList("Method Count", "Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pClassFileDataMap));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private HTMLElement createBreadcrumbs(File pWorkDir) {
        List<String> split = new ArrayList<>(Arrays.asList(pWorkDir.toPath().relativize(baseDir.toPath()).toString().split("/")));
        HTMLElement breadcrumbs = HTMLElement.div();
        while (split.size() > 0 && !split.get(0).equals("")) {
            File parent = pWorkDir.getParentFile();
            int parentCounter = split.size();
            for (int i = 1; i < parentCounter; i++) {
                parent = parent.getParentFile();
            }
            if (!pWorkDir.getParentFile().equals(baseDir)) {
                split.set(0, ".");
            }
            String link = String.format("%s/index.html", String.join("/", split));
            breadcrumbs.getContent().add(HTMLElement.a(link, parent.getName()));
            breadcrumbs.getContent().add(HTMLElement.noTag(" > "));
            split.remove(0);
        }
        breadcrumbs.getContent().add(HTMLElement.span(pWorkDir.getName()));
        return breadcrumbs;
    }

    private HTMLElement createClassOverviewHTML(final ClassExecutionData pData,
                                                final File pClassFile,
                                                final String pClassFileName,
                                                final String pPathToStyleSheet,
                                                final String pPathToScript) {
        HTMLElement htmlMainTag = HTMLElement.html();
        String classFileName = String.format("%s.java", pClassFileName);
        htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, pPathToStyleSheet));
        HTMLElement htmlBodyTag = HTMLElement.body();
        htmlBodyTag.getAttributes().add("onload=\"sortTables()\"");
        htmlBodyTag.getContent().add(HTMLElement.script("text/javascript", pPathToScript));
        htmlBodyTag.getContent().add(createBreadcrumbs(pClassFile));
        htmlBodyTag.getContent().add(HTMLElement.h1(pClassFileName));
        List<String> columns = new ArrayList<>(Arrays.asList("Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pData, pClassFileName));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private HTMLElement processLine(String pClassName, int lineNumber, String lineString, ClassExecutionData data) {
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

        // PER WORD
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
                List<ProgramVariable> definitions = new ArrayList<>();
                definitions.add(definition);
                for (Pair<ProgramVariable, ProgramVariable> match : data.getParameterMatching()) {
                    if (match.fst.equals(definition)) {
                        definitions.add(match.snd);
                    }
                }
                Map<ProgramVariable, Boolean> useCoverageMap = new HashMap<>();
                for (ProgramVariable def : definitions) {
                    useCoverageMap.putAll(getUseCoverageMap(data, def));
                }
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
                    } else {
                        spanTag.getAttributes().add("class=\"red\"");
                    }
                } else {
                    if (isRedefined(data, lineNumber, word)) {
                        spanTag = HTMLElement.span(word);
                        // TODO: Create dropdown for variables being redefined with reference to up-to-date definitions
                        spanTag.getAttributes().add("class=\"overwritten\"");
                    }
                    addCodeHighlighting(spanTag, word);
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

//    private String getRedefinitionBackgroundColorHex(ClassExecutionData pData, int pLineNumber, String pName) {
//        boolean covered = false;
//        boolean uncovered = false;
//        for (Map.Entry<String, Set<ProgramVariable>> entry : pData.getDefUseCovered().entrySet()) {
//            for (ProgramVariable programVariable : entry.getValue()) {
//                pData.getDefUseUncovered().get(entry.getKey())
//                if (programVariable.getLineNumber() > pLineNumber
//                        && programVariable.getName().equals(pName)
//                        && pData.getMethodRangeMap().get(entry.getKey()).fst < pLineNumber
//                        && pData.getMethodRangeMap().get(entry.getKey()).snd > pLineNumber) {
//                    covered = true;
//                }
//            }
//        }
//
//        if (uncovered && !covered) {
//            return "red";
//        } else if (covered && !uncovered) {
//            return "blue";
//        } else if (covered) {
//            return "pink";
//        } else {
//            return "";
//        }
//    }

    private HTMLElement createDefaultHTMLHead(final String pTitle,
                                              final String pPathToResources) {
        HTMLElement headTag = HTMLElement.head();
        headTag.getContent().add(
                HTMLElement.link("stylesheet", pPathToResources, "text/css"));
        headTag.getContent().add(
                HTMLElement.title(pTitle));
        return headTag;
    }

    private HTMLElement createDataTable(final List<String> pColumns, Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getContent().add(createTableHeadTag(pColumns));
        tableTag.getContent().add(createTableBodyTag(pClassFileDataMap));
        tableTag.getContent().add(createTableFootTag(pClassFileDataMap));
        return tableTag;
    }

    private HTMLElement createDataTable(final List<String> pColumns,
                                        final ClassExecutionData pData,
                                        final String pClassfileName) {
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getContent().add(createTableHeadTag(pColumns));
        for (Map.Entry<String, List<DefUsePair>> entry : pData.getDefUsePairs().entrySet()) {
            String elementName = entry.getKey();
            if (elementName.contains("<init>")) {
                elementName = elementName.replace("<init>", "init");
            }
            String total = "createDataTable";
            String covered = "createDataTable";
            String missed = "createDataTable";
            String link = String.format("%s.java.html#L%s", pClassfileName,
                    pData.getMethodFirstLine().get(entry.getKey()));
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

    private HTMLElement createTableHeadTag(final List<String> pColumns) {
        HTMLElement theadTag = HTMLElement.thead();
        theadTag.getContent().add(HTMLElement.td("Element"));
        for (String col : pColumns) {
            theadTag.getContent().add(HTMLElement.td(col));
        }
        return theadTag;
    }

    private HTMLElement createTableFootTag(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement tfootTag = HTMLElement.tfoot();
        Map.Entry<String, ExecutionDataNode<ExecutionData>> entry = pClassFileDataMap.entrySet().iterator().next();
        ExecutionData parentData = entry.getValue().getParent().getData();
        tfootTag.getContent().add(HTMLElement.td("Total"));
        tfootTag.getContent().add(HTMLElement.td(parentData.getMethodCount()));
        tfootTag.getContent().add(HTMLElement.td(parentData.getTotal()));
        tfootTag.getContent().add(HTMLElement.td(parentData.getCovered()));
        tfootTag.getContent().add(HTMLElement.td(parentData.getTotal() - parentData.getCovered()));
        return tfootTag;
    }

    private HTMLElement createTableFootTag(final ClassExecutionData pData) {
        HTMLElement tfootTag = HTMLElement.tfoot();
        tfootTag.getContent().add(HTMLElement.td("Total"));
        tfootTag.getContent().add(HTMLElement.td(pData.getTotal()));
        tfootTag.getContent().add(HTMLElement.td(pData.getCovered()));
        tfootTag.getContent().add(HTMLElement.td(pData.getTotal() - pData.getCovered()));
        return tfootTag;
    }

    private HTMLElement createTableBodyTag(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
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
            trTag.getContent().add(HTMLElement.td(data.getTotal() - data.getCovered()));
            bodyTag.getContent().add(trTag);
        }
        return bodyTag;
    }

    private HTMLElement createTooltip(final String pClassName, final Map<ProgramVariable, Boolean> pUses, final String pText) {
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

    private void addCodeHighlighting(HTMLElement pSpanTag, String pWord) {
        if (isJavaKeyword(pWord)) {
            pSpanTag.getAttributes().add("class=\"java-keyword\"");
        } else if (isTypeKeyword(pWord)) {
            pSpanTag.getAttributes().add("class=\"type-keyword\"");
        }
    }

    private boolean isJavaKeyword(String pWord) {
        return (Arrays.binarySearch(JAVA_KEYWORDS, pWord) >= 0);
    }

    private boolean isTypeKeyword(String pWord) {
        return (Arrays.binarySearch(TYPE_KEYWORDS, pWord) >= 0);
    }

    private String getDefinitionBackgroundColorHex(boolean pDefCovered, Map<ProgramVariable, Boolean> pUses) {
        if (!pDefCovered || pUses.size() == 0 || Collections.frequency(pUses.values(), true) == 0) {
            return "red";
        } else if (Collections.frequency(pUses.values(), true) == pUses.size()) {
            return "green";
        } else {
            return "yellow";
        }
    }

    // find definition by line number and name
    private ProgramVariable findDefinition(ClassExecutionData pData, int pLineNumber, String pName) {
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

    private boolean isRedefined(ClassExecutionData pData, int pLineNumber, String pName) {
        // TODO: Expand functionality to whole class for instance variables.
        //  Therefore range map of instance variables (so to see the "method" that can be seen as class
        //  instance needs to be appropriately updated when a local variable with the same name exists.
//        if (isInstanceVariable(pData, pLineNumber, pName)) {
//            for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
//                for (DefUsePair defUsePair : defUsePairs.getValue()) {
//                    ProgramVariable definition = defUsePair.getDefinition();
//                    if (definition.getName().equals(pName) && ) {
//                        return true;
//                    }
//                }
//            }
//        } else {
//        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
//            for (DefUsePair defUsePair : defUsePairs.getValue()) {
//                ProgramVariable definition = defUsePair.getDefinition();
//                if (definition.getLineNumber() > pLineNumber
//                        && definition.getName().equals(pName)
//                        && pData.getInstVarOutScopeMap().get(defUsePairs.getKey()).fst <= pLineNumber
//                        && pData.getInstVarOutScopeMap().get(defUsePairs.getKey()).snd >= pLineNumber) {
//                    return true;
//                }
//            }
//        }
//        }
        return false;
    }

    private boolean isInstanceVariable(ClassExecutionData pData, int pLineNumber, String pName) {
        for (InstanceVariable instanceVariable : pData.getInstanceVariables()) {
            if (instanceVariable.getName().equals(pName) && instanceVariable.getLineNumber() > pLineNumber) {
                return true;
            }
        }
        return false;
    }

    private ProgramVariable findUsage(ClassExecutionData pData, int pLineNumber, String pName) {
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

    private boolean findIsDefCovered(ClassExecutionData pData, ProgramVariable pDefinition) {
        for (Map.Entry<String, Set<ProgramVariable>> defUseCovered : pData.getDefUseCovered().entrySet()) {
            if (defUseCovered.getValue().contains(pDefinition)) {
                return true;
            }
        }
        return false;
    }

    // find all uses for one particular definition
    private Map<ProgramVariable, Boolean> getUseCoverageMap(ClassExecutionData pData, ProgramVariable pDefinition) {
        Map<ProgramVariable, Boolean> uses = new HashMap<>();
        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for (DefUsePair defUsePair : defUsePairs.getValue()) {
                if (defUsePair.getDefinition().equals(pDefinition)) {
                    ProgramVariable use = defUsePair.getUsage();
                    boolean covered = isCovered(pData, use);
                    uses.put(use, covered);
                }
            }
        }
        return uses;
    }

    private boolean isCovered(ClassExecutionData pData, ProgramVariable pUsage) {
        boolean useCovered = false;
        boolean allDefsCovered = true;
        // Check if usage is covered
        for (Map.Entry<String, Set<ProgramVariable>> entry : pData.getDefUseCovered().entrySet()) {
            if (entry.getValue().contains(pUsage)) {
                useCovered = true;

                // based on found usage check if all depending defs are covered
                for (DefUsePair pair : pData.getDefUsePairs().get(entry.getKey())) {
                    if (pair.getUsage().equals(pUsage)) {
                        allDefsCovered = allDefsCovered && entry.getValue().contains(pair.getDefinition());
                    }
                }
            }
        }
        return useCovered && allDefsCovered;
    }
}
