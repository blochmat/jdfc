package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.ifg.data.DefUsePair;
import com.jdfc.core.analysis.ifg.data.InstanceVariable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.report.html.resources.Resources;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void createIndex(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
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

    private HTMLElement createIndexHTML(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap,
                                        final File pWorkDir,
                                        final String pPathToStyleSheet,
                                        final String pPathToScript) {
        String[] split = pWorkDir.toString().split("/");
        String title = split[split.length - 1];

        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(title, pPathToStyleSheet));

        HTMLElement htmlBodyTag = createDefaultHTMLBody(title, pWorkDir, pPathToScript, null);
        List<String> columns = new ArrayList<>(Arrays.asList("Method Count", "Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pClassFileDataMap));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    public void createClassOverview(final String pClassName,
                                    final ExecutionData pData,
                                    final File pWorkDir) throws IOException {
        if (pData instanceof ClassExecutionData) {
            String filePath = String.format("%s/%s.html", pWorkDir.toString(), pClassName);
            File classFile = new File(filePath);

            String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(classFile), STYLE_SHEET);
            String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(classFile), SCRIPT);

            HTMLElement classOverviewHTML =
                    createClassOverviewHTML((ClassExecutionData) pData, classFile, pClassName, styleSheetPath, scriptPath);

            Writer writer = new FileWriter(classFile);
            writer.write(classOverviewHTML.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from ExecutionData instance.");
        }
    }

    private HTMLElement createClassOverviewHTML(final ClassExecutionData pData,
                                                final File pClassFile,
                                                final String pClassFileName,
                                                final String pPathToStyleSheet,
                                                final String pPathToScript) {
        String classFileName = String.format("%s.java", pClassFileName);

        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, pPathToStyleSheet));

        HTMLElement htmlBodyTag = createDefaultHTMLBody(pClassFileName, pClassFile, pPathToScript, null);
        List<String> columns = new ArrayList<>(Arrays.asList("Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createDataTable(columns, pData, pClassFileName));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private HTMLElement createBreadcrumbs(File pWorkDir) {
        List<String> split =
                new ArrayList<>(Arrays.asList(pWorkDir.toPath().relativize(baseDir.toPath()).toString().split("/")));
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

    public void createClassSourceView(final String pClassName,
                                      final ExecutionData pData,
                                      final File pWorkDir,
                                      final File pSourceDir)
            throws IOException {
        if (pData instanceof ClassExecutionData) {
            String sourceViewPath = String.format("%s/%s.java.html", pWorkDir.toString(), pClassName);
            File sourceViewHTML = new File(sourceViewPath);

            String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(sourceViewHTML), STYLE_SHEET);
            String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(sourceViewHTML), SCRIPT);

            String classFilePath = String.format("%s/%s.java", pSourceDir.toString(), ((ClassExecutionData) pData).getRelativePath());
            File classFile = new File(classFilePath);

            HTMLElement classSourceViewHTML =
                    createClassSourceViewHTML(classFile, (ClassExecutionData) pData, pClassName, styleSheetPath, scriptPath);

            Writer writer = new FileWriter(sourceViewHTML);
            writer.write(classSourceViewHTML.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from ExecutionData instance.");
        }
    }

    private HTMLElement createClassSourceViewHTML(final File pClassFile,
                                                  final ClassExecutionData pData,
                                                  final String pClassName,
                                                  final String pPathToStyleSheet,
                                                  final String pPathToScript) {
        String classFileName = String.format("%s.java", pClassName);
        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, pPathToStyleSheet));

        HTMLElement htmlBodyTag = createDefaultHTMLBody(pClassName, null, pPathToScript, "code");
        htmlMainTag.getContent().add(htmlBodyTag);
        try {
            htmlBodyTag.getContent().add(createSourceCode(pClassFile, pData, pClassName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return htmlMainTag;
    }

    private HTMLElement createSourceCode(final File pClassFile,
                                         final ClassExecutionData pData,
                                         final String pClassName) throws FileNotFoundException {
        Scanner scanner = new Scanner(pClassFile);
        HTMLElement table = HTMLElement.table();
        table.getAttributes().add("id=\"classDetailView\"");
        int currentLineCounter = 0;
        while (scanner.hasNextLine()) {
            currentLineCounter += 1;
            String currentLineString = scanner.nextLine();

            HTMLElement row = HTMLElement.tr();
            table.getContent().add(row);

            HTMLElement sourceCodeLine = HTMLElement.td(currentLineCounter);
            sourceCodeLine.getAttributes().add(String.format("id=\"L%s\"", currentLineCounter));
            row.getContent().add(sourceCodeLine);

            HTMLElement sourceCodeText = HTMLElement.td();
            row.getContent().add(sourceCodeText);

            HTMLElement finalizedText = finalizeText(pClassFile, pClassName, currentLineCounter, currentLineString, pData);
            sourceCodeText.getContent().add(finalizedText);
        }
        scanner.close();
        return table;
    }

    private HTMLElement finalizeText(final File pClassFile,
                                     final String pClassName,
                                     final int pLineNumber,
                                     final String pLineString,
                                     final ClassExecutionData pData) {
        HTMLElement divTagLine = HTMLElement.div();
        divTagLine.getAttributes().add("class=\"line\"");

        List<String> specialChars = extractChars(pLineString, "\\w+\\b");
        List<String> words = extractChars(pLineString, "\\W+");
        List<String> workList = createWorkList(words, specialChars);

        while (!workList.isEmpty()) {
            String item = workList.get(0);
            workList.remove(0);

            Pattern specCharPattern = Pattern.compile("\\W+");
            Matcher specCharMatcher = specCharPattern.matcher(item);

            if (specCharMatcher.matches()) {
                if (item.contains("//")) {
                    divTagLine.getContent().add(createCommentSpan(item, workList));
                } else {
                    divTagLine.getContent().add(HTMLElement.pre(item));
                }
            } else {
                int sameWordsLeft = (int) workList.stream().filter(x -> x.equals(item)).count();
                ProgramVariable programVariable = findProgramVariable(pData, pLineNumber, item, sameWordsLeft);
                if (programVariable == null) {
                    HTMLElement spanTag = HTMLElement.span(item);
                    divTagLine.getContent().add(spanTag);
                    addCodeHighlighting(spanTag, item);

                } else {
                    if (isRedefined(pData, pLineNumber, item)) {
                        HTMLElement spanTag = HTMLElement.span(item);
                        divTagLine.getContent().add(spanTag);
                        spanTag.getAttributes().add("class=\"overwritten\"");
                    } else {
                        boolean isVarCovered = isVarCovered(pData, programVariable);
                        List<ProgramVariable> correlatedVars = getCorrelatedVars(programVariable, pData);
                        Map<DefUsePair, Boolean> defusePairsCovered = getDefUsePairCovered(pData, correlatedVars);
                        String backgroundColor = getVariableBackgroundColor(isVarCovered, defusePairsCovered);
                        try {
                            divTagLine.getContent().add(
                                    createVariableInformation(pData, pClassFile, pClassName,
                                            defusePairsCovered, programVariable, backgroundColor));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return divTagLine;
    }

    private List<String> extractChars(String pLineString, String pRegex) {
        String[] extract = pLineString.split(pRegex);
        List<String> list = new ArrayList<>();
        for (String str : extract) {
            if (!str.equals("")) {
                list.add(str);
            }
        }
        return list;
    }

    private List<String> createWorkList(List<String> pWords, List<String> pSpecialChars) {
        List<String> workListWords = new ArrayList<>(pWords);
        List<String> workListSpec = new ArrayList<>(pSpecialChars);

        boolean isSpecialCharsLonger = false;
        boolean isCommentLine = false;

        if (!workListSpec.isEmpty()) {
            isSpecialCharsLonger = workListSpec.size() > workListWords.size();
            isCommentLine = workListSpec.get(0).contains("//");
        }

        List<String> result = new ArrayList<>();

        while (!workListWords.isEmpty()) {
            // Inline comment
            if (workListSpec.get(0).contains("//") && workListWords.size() < pWords.size()) {
                int index = workListSpec.get(0).indexOf("//");
                String remainder = workListSpec.get(0).substring(0, index);
                String commentSlashes = workListSpec.get(0).substring(index);

                result.add(remainder);
                result.add(commentSlashes);
                result.add(workListWords.get(0));

                workListSpec.remove(0);
                workListWords.remove(0);

            } else if (isSpecialCharsLonger || isCommentLine) {
                result.add(workListSpec.get(0));
                result.add(workListWords.get(0));
                workListWords.remove(0);
                workListSpec.remove(0);

            } else {
                result.add(workListWords.get(0));
                result.add(workListSpec.get(0));
                workListWords.remove(0);
                workListSpec.remove(0);
            }
        }

        if (isSpecialCharsLonger) {
            result.add(workListSpec.get(0));
            workListSpec.remove(0);
        }

        return result;
    }

    private HTMLElement createCommentSpan(String pCommentSlashItem, List<String> pRemainder) {
        HTMLElement comment = HTMLElement.pre();
        comment.getAttributes().add("class=\"comment\"");
        comment.getContent().add(HTMLElement.noTag(pCommentSlashItem));
        while (!pRemainder.isEmpty()) {
            comment.getContent().add(HTMLElement.noTag(pRemainder.get(0)));
            pRemainder.remove(0);
        }
        return comment;
    }

    private List<ProgramVariable> getCorrelatedVars(ProgramVariable pVariable, ClassExecutionData pData) {
        List<ProgramVariable> result = new ArrayList<>();
        result.add(pVariable);
        for (Map.Entry<ProgramVariable, ProgramVariable> entry : pData.getInterProceduralMatches().entrySet()) {
            if (entry.getKey().equals(pVariable)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    private Map<DefUsePair, Boolean> getDefUsePairCovered(ClassExecutionData pData, List<ProgramVariable> pCorrelatedVars) {
        Map<DefUsePair, Boolean> result = new HashMap<>();
        for (ProgramVariable element : pCorrelatedVars) {
            result.putAll(getDefUsePairsCoveredForVar(pData, element));
        }
        return result;
    }

    private Map<DefUsePair, Boolean> getUseDefPairCoveredForDef(ClassExecutionData pData, ProgramVariable pDefinition) {
        Map<DefUsePair, Boolean> result = new HashMap<>();
        for (Map.Entry<String, Map<DefUsePair, Boolean>> duCoveredMapEntry : pData.getDefUsePairsCovered().entrySet()) {
            for (Map.Entry<DefUsePair, Boolean> element : duCoveredMapEntry.getValue().entrySet()) {
                if (element.getKey().getUsage().equals(pDefinition)) {
                    result.put(element.getKey(), element.getValue());
                }
            }
        }
        return result;
    }

//    private Map<ProgramVariable, Boolean> createUseCoverageMap(List<ProgramVariable> pCorrespondingDefsList, ClassExecutionData pData) {
//        Map<ProgramVariable, Boolean> useCoverageMap = new HashMap<>();
//        for (ProgramVariable definition : pCorrespondingDefsList) {
//            useCoverageMap.putAll(findDefUsePairCovered(pData, definition));
//        }
//        return useCoverageMap;
//    }

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

    private HTMLElement createDefaultHTMLBody(String pTitle, File pFile, String pPathToScript, String pStyleClass) {
        HTMLElement htmlBodyTag = HTMLElement.body();
        if (pStyleClass != null) {
            String styleClass = String.format("class=\"%s\"", pStyleClass);
            htmlBodyTag.getAttributes().add(styleClass);
        }
        htmlBodyTag.getAttributes().add("onload=\"sortTables()\"");
        htmlBodyTag.getContent().add(HTMLElement.script("text/javascript", pPathToScript));
        if (pFile != null) {
            htmlBodyTag.getContent().add(createBreadcrumbs(pFile));
        }
        htmlBodyTag.getContent().add(HTMLElement.h1(pTitle));
        return htmlBodyTag;
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

            // Methods with 0 DefUsePairs are standard object constructors; we do not want to show those.
            if (entry.getValue().size() != 0) {
                if (elementName.contains("<init>")) {
                    elementName = elementName.replace("<init>", "init");
                }
                int total = entry.getValue().size();
                int covered = pData.computeCoveredForMethod(entry.getKey());
                int missed = total - covered;
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
        }
        tableTag.getContent().add(createTableFootTag(pData));
        return tableTag;
    }

    private HTMLElement createTableHeadTag(final List<String> pColumns) {
        HTMLElement theadTag = HTMLElement.thead();
        HTMLElement rowTag = HTMLElement.tr();
        theadTag.getContent().add(rowTag);
        rowTag.getContent().add(HTMLElement.td("Element"));
        for (String col : pColumns) {
            rowTag.getContent().add(HTMLElement.td(col));
        }
        return theadTag;
    }

    private HTMLElement createTableFootTag(final Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement tfootTag = HTMLElement.tfoot();
        HTMLElement rowTag = HTMLElement.tr();
        tfootTag.getContent().add(rowTag);
        Map.Entry<String, ExecutionDataNode<ExecutionData>> entry = pClassFileDataMap.entrySet().iterator().next();
        ExecutionData parentData = entry.getValue().getParent().getData();
        rowTag.getContent().add(HTMLElement.td("Total"));
        rowTag.getContent().add(HTMLElement.td(parentData.getMethodCount()));
        rowTag.getContent().add(HTMLElement.td(parentData.getTotal()));
        rowTag.getContent().add(HTMLElement.td(parentData.getCovered()));
        rowTag.getContent().add(HTMLElement.td(parentData.getTotal() - parentData.getCovered()));
        return tfootTag;
    }

    private HTMLElement createTableFootTag(final ClassExecutionData pData) {
        HTMLElement tfootTag = HTMLElement.tfoot();
        HTMLElement rowTag = HTMLElement.tr();
        tfootTag.getContent().add(rowTag);
        rowTag.getContent().add(HTMLElement.td("Total"));
        rowTag.getContent().add(HTMLElement.td(pData.getTotal()));
        rowTag.getContent().add(HTMLElement.td(pData.getCovered()));
        rowTag.getContent().add(HTMLElement.td(pData.getTotal() - pData.getCovered()));
        return tfootTag;
    }

    private HTMLElement createTableBodyTag(Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        HTMLElement bodyTag = HTMLElement.tbody();
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

    private HTMLElement createVariableInformation(final ClassExecutionData pData,
                                                  final File pClassFile,
                                                  final String pClassName,
                                                  final Map<DefUsePair, Boolean> pDefUseCovered,
                                                  final ProgramVariable pVariable,
                                                  final String pBackgroundColor) throws FileNotFoundException {
        boolean isDefinition = isDefinition(pVariable, pDefUseCovered.keySet());
        boolean isUsage = isUsage(pVariable, pDefUseCovered.keySet());
        // create table and show colored entries on hover over
        // entries are links to the variable use
        HTMLElement variableDiv = HTMLElement.div();
        variableDiv.getAttributes().add("class=\"variable\"");
        HTMLElement variableSpan = HTMLElement.span(pVariable.getName());
        variableDiv.getContent().add(variableSpan);
        String variableSpanAttr = String.format("class=\"link %s\"", pBackgroundColor);
        variableSpan.getAttributes().add(variableSpanAttr);

//        try {
        HTMLElement tooltipDiv = HTMLElement.div();
        variableDiv.getContent().add(tooltipDiv);
        tooltipDiv.getAttributes().add("class=\"tooltipDef red rounded-corners\"");
        HTMLElement table = HTMLElement.table();
        tooltipDiv.getContent().add(table);
        table.getAttributes().add("class=\"yellow rounded-corners\"");
        table.getContent().add(createTabButtons(pVariable, isDefinition, isUsage));

        if (isDefinition) {
            Set<ProgramVariable> correlatedDefs = extractCorrelatedDefs(pDefUseCovered, pVariable);
            table.getContent().add(createCorrelatedDefInfo(pClassName, pVariable, correlatedDefs));
            table.getContent().add(createTabInfoTable(pClassFile, pClassName, pVariable, pDefUseCovered, true));
        }

        if(isUsage) {
            table.getContent().add(createTabInfoTable(pClassFile, pClassName, pVariable, pDefUseCovered, false));
        }

        return variableDiv;
    }

//    private HTMLElement createTooltip(final ClassExecutionData pData,
//                                      final File pClassFile,
//                                      final String pClassName,
//                                      final Map<DefUsePair, Boolean> pUses,
//                                      final Map<DefUsePair, Boolean> pDefs,
//                                      final ProgramVariable pVariable) throws FileNotFoundException {
//        HTMLElement tooltipDiv = HTMLElement.div();
//        tooltipDiv.getAttributes().add("class=\"tooltipDef red rounded-corners\"");
//        HTMLElement table = HTMLElement.table();
//        tooltipDiv.getContent().add(table);
//        table.getAttributes().add("class=\"yellow rounded-corners\"");
//        table.getContent().add(createTabButtons(pVariable, pUses, pDefs));
//
//        // Definition tab
//        Set<ProgramVariable> correspondingDefs = extractCorrespondingDefs(pUses, pVariable);
//        if (!correspondingDefs.isEmpty()) {
//            table.getContent().add(createDefInfo(pClassName, correspondingDefs, true));
//        }
//        table.getContent().add(createInformationTable(pClassFile, pClassName, pData, pUses, true));
//
//        // Usage tab
//        Set<ProgramVariable> possibleDefinitions = extractPossibleDefs(pDefs);
//        if (!possibleDefinitions.isEmpty()) {
//            table.getContent().add(createDefInfo(pClassName, possibleDefinitions, false));
//        }
//        table.getContent().add(createInformationTable(pClassFile, pClassName, pData, pUses, false));
//
//        return tooltipDiv;
//    }

    private HTMLElement createTabInfoTable(final File pClassFile,
                                           final String pClassName,
                                           final ProgramVariable pVariable,
                                           final Map<DefUsePair, Boolean> pVariableInfo,
                                           final boolean isDefinitionTab) throws FileNotFoundException {
        HTMLElement row = HTMLElement.tr();
        HTMLElement cell = HTMLElement.td();
        row.getContent().add(cell);
        HTMLElement div = HTMLElement.div();
        cell.getContent().add(div);
        HTMLElement table = HTMLElement.table();
        div.getContent().add(table);
        table.getAttributes().add("class=\"tooltipTable yellow\"");
        table.getContent().add(createHeaderRow());

        Map<ProgramVariable, Boolean> variableCovered;
        if (isDefinitionTab) {
            String defTab = String.format("class=\"%s%s%sDefTab margin10 green\"", pVariable.getName(),
                    pVariable.getLineNumber(), pVariable.getInstructionIndex());
            div.getAttributes().add(defTab);
        } else {
            String useTab = String.format("class=\"%s%s%sUseTab margin10 green\"", pVariable.getName(),
                    pVariable.getLineNumber(), pVariable.getInstructionIndex());
            div.getAttributes().add(useTab);
        }
        variableCovered = getCoverageInformation(pVariableInfo, isDefinitionTab);

        for (Map.Entry<ProgramVariable, Boolean> element : variableCovered.entrySet()) {
            table.getContent().add(createDataRow(pClassFile, pClassName, element.getKey(), element.getValue()));
        }

        return row;
    }

    private Map<ProgramVariable, Boolean> getCoverageInformation(final Map<DefUsePair, Boolean> pVariableInfo,
                                                                 final boolean isDefinitionTab) {
        Map<ProgramVariable, Boolean> result = new TreeMap<>();
        for (Map.Entry<DefUsePair, Boolean> entry : pVariableInfo.entrySet()) {
            if(isDefinitionTab) {
                ProgramVariable usage = entry.getKey().getUsage();
                result.put(usage, entry.getValue());
            } else {
                ProgramVariable definition = entry.getKey().getDefinition();
                result.put(definition, entry.getValue());
            }
        }
        return result;
    }

    private Map<ProgramVariable, Boolean> getDefinitionsCoverageInformation(final ClassExecutionData pData,
                                                                            final Map<DefUsePair, Boolean> pVariableInfo) {
        Map<ProgramVariable, Boolean> result = new HashMap<>();
        for (DefUsePair element : pVariableInfo.keySet()) {
            ProgramVariable definition = element.getDefinition();
            Boolean covered = isVariableCovered(pData, definition);
            result.put(definition, covered);
        }
        return result;
    }

    private HTMLElement createDataRow(final File pClassFile,
                                      final String pClassName,
                                      final ProgramVariable pVariable,
                                      final Boolean pCovered) throws FileNotFoundException {
        HTMLElement row = HTMLElement.tr();
        HTMLElement lineCell = HTMLElement.td(pVariable.getLineNumber());
        lineCell.getAttributes().add("class=\"rightBorder centerText\"");
        row.getContent().add(lineCell);

        HTMLElement statementCell = HTMLElement.td();
        statementCell.getAttributes().add("class=\"nowrap\"");
        row.getContent().add(statementCell);

        String link = String.format("%s.java.html#L%s", pClassName, pVariable.getLineNumber());
        String statement = getLineFromFile(pClassFile, pVariable.getLineNumber());
        HTMLElement a = HTMLElement.a(link, statement);
        statementCell.getContent().add(a);
        if (pCovered) {
            a.getAttributes().add("class=\"green\"");
        } else {
            a.getAttributes().add("class=\"red\"");
        }

        return row;
    }

    private HTMLElement createHeaderRow() {
        HTMLElement row = HTMLElement.tr();
        HTMLElement lineCell = HTMLElement.td("Line");
        lineCell.getAttributes().add("class=\"tableHead rightBorder centerText\"");
        row.getContent().add(lineCell);

        HTMLElement statementCell = HTMLElement.td("Statement");
        statementCell.getAttributes().add("class=\"tableHead centerText\"");
        row.getContent().add(statementCell);

        return row;
    }

    private HTMLElement createTabButtons(final ProgramVariable pVariable,
                                         final boolean isDefinition,
                                         final boolean isUsage) {
        HTMLElement row = HTMLElement.tr();
        HTMLElement cell = HTMLElement.td();
        row.getContent().add(cell);
        HTMLElement div = HTMLElement.div();
        cell.getContent().add(div);
        div.getAttributes().add("class=\"buttonContainer overwritten rounded-corners\"");


        HTMLElement definitionButton = null;
        if (isDefinition) {
            definitionButton = HTMLElement.button("Definition");
            String id = String.format("id=\"%s%s%sDefTabButton\"", pVariable.getName(), pVariable.getLineNumber(), pVariable.getInstructionIndex());
            definitionButton.getAttributes().add(id);
            definitionButton.getAttributes().add("onclick=\"openTab(this)\"");
            definitionButton.getAttributes().add("class=\"defaultOpen inner\"");
            div.getContent().add(definitionButton);
        }

        HTMLElement usageButton;
        if (isUsage) {
            usageButton = HTMLElement.button("Usage");
            String id = String.format("id=\"%s%s%sUseTabButton\"", pVariable.getName(), pVariable.getLineNumber(), pVariable.getInstructionIndex());
            usageButton.getAttributes().add(id);
            usageButton.getAttributes().add("onclick=\"openTab(this)\"");
            if (definitionButton == null) {
                usageButton.getAttributes().add("class=\"defaultOpen inner\"");
            } else {
                usageButton.getAttributes().add("class=\" inner\"");
            }
            div.getContent().add(usageButton);
        }
        return row;
    }

    private Set<ProgramVariable> extractCorrelatedDefs(final Map<DefUsePair, Boolean> pDefUseCovered,
                                                       final ProgramVariable pVariable) {
        Set<ProgramVariable> result = new HashSet<>();
        for (DefUsePair element : pDefUseCovered.keySet()) {
            if (!element.getDefinition().equals(pVariable)) {
                result.add(element.getDefinition());
            }
        }
        return result;
    }

    private Set<ProgramVariable> extractPossibleDefs(final Map<DefUsePair, Boolean> pDefinitions) {
        Set<ProgramVariable> result = new HashSet<>();
        for (DefUsePair element : pDefinitions.keySet()) {
            result.add(element.getDefinition());
        }
        return result;
    }

    private HTMLElement createCorrelatedDefInfo(final String pClassName,
                                                final ProgramVariable pVariable,
                                                final Set<ProgramVariable> pVariables) {
        HTMLElement row = HTMLElement.tr();
        HTMLElement cell = HTMLElement.td();
        row.getContent().add(cell);

        HTMLElement div = HTMLElement.div();
        String defTab = String.format("class=\"%s%s%sDefTab\"", pVariable.getName(),
                pVariable.getLineNumber(), pVariable.getInstructionIndex());
        div.getAttributes().add(defTab);
        cell.getContent().add(div);

        HTMLElement span = HTMLElement.span("Correlated Definitions:");
        span.getAttributes().add("class=\"margin10 nowrap\"");
        div.getContent().add(span);
        div.getContent().add(HTMLElement.pre(" "));

        if (pVariables.isEmpty()) {
            div.getContent().add(HTMLElement.span("None"));
        }

        for (ProgramVariable element : pVariables) {
            String link = String.format("%s.java.html#L%s", pClassName, element.getLineNumber());
            div.getContent().add(HTMLElement.a(link, element.getName()));
            div.getContent().add(HTMLElement.pre(" "));
        }
        return row;
    }


    private HTMLElement createVariableInformation(String pText, boolean pUseCovered, boolean pAllDefsCovered) {
        String tooltipString;
        if (!pUseCovered && !pAllDefsCovered) {
            tooltipString = "Use and Definition(s) uncovered";
        } else if (!pUseCovered) {
            tooltipString = "Use uncovered";
        } else {
            tooltipString = "Definition(s) uncovered";
        }
        HTMLElement tooltip = HTMLElement.div();
        tooltip.getAttributes().add("class=\"tooltip red\"");
        tooltip.getContent().add(HTMLElement.span(pText));
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getAttributes().add("class=\"tooltipcontent\"");
        tooltip.getContent().add(tableTag);
        HTMLElement tbody = HTMLElement.tbody();
        tableTag.getContent().add(tbody);
        tbody.getContent().add(createTypeRow(false));
        HTMLElement tr = HTMLElement.tr();
        tbody.getContent().add(tr);
        HTMLElement td = HTMLElement.td(tooltipString);
        tr.getContent().add(td);
        td.getAttributes().add("class=\"red tooltipcell\"");
        return tooltip;
    }

    private HTMLElement createColumnIdentifier() {
        HTMLElement tr = HTMLElement.tr();
        tr.getContent().add(HTMLElement.td("Line"));
        tr.getContent().add(HTMLElement.td("Statement"));
        return tr;
    }

    private HTMLElement createTypeRow(boolean pIsDefinition) {
        HTMLElement tr = HTMLElement.tr();
        tr.getAttributes().add("class=\"headerRow\"");
        if (pIsDefinition) {
            tr.getContent().add(HTMLElement.td("Type: Definition"));
        } else {
            tr.getContent().add(HTMLElement.td("Type: Usage"));
        }
        return tr;
    }

    private String getLineFromFile(final File pClassFile,
                                   final int pLineNumber) throws FileNotFoundException {
        Scanner scanner = new Scanner(pClassFile);
        String lineString = null;
        int lineCounter = pLineNumber;
        while (scanner.hasNextLine() && lineCounter != 0) {
            lineString = scanner.nextLine();
            lineCounter--;
        }
        return lineString;
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

    private String getVariableBackgroundColor(boolean pIsVarCovered, Map<DefUsePair, Boolean> pDefUsePairCovered) {
        if (!pIsVarCovered || pDefUsePairCovered.size() == 0 || Collections.frequency(pDefUsePairCovered.values(), true) == 0) {
            return "red";
        } else if (Collections.frequency(pDefUsePairCovered.values(), true) == pDefUsePairCovered.size()) {
            return "green";
        } else {
            return "yellow";
        }
    }

    // find definition by line number and name
    private ProgramVariable findProgramVariable(ClassExecutionData pData, int pLineNumber, String pName, int pSameWordsLeft) {
        List<ProgramVariable> possibleDefUsePairs = getPossibleVars(pData, pLineNumber, pName);
        if (!possibleDefUsePairs.isEmpty()) {
            possibleDefUsePairs.sort(Comparator.comparing(ProgramVariable::getInstructionIndex));
            int index = (possibleDefUsePairs.size() - 1) - pSameWordsLeft;
            return possibleDefUsePairs.get(index);
        }

        List<InstanceVariable> instanceVarDefinitions = getPossibleInstanceVarDefinitions(pData.getInstanceVariables(), pLineNumber, pName);
        if (!instanceVarDefinitions.isEmpty()) {
            instanceVarDefinitions.sort(Comparator.comparing(InstanceVariable::getInstructionIndex));
            int index = (instanceVarDefinitions.size() - 1) - pSameWordsLeft;
            InstanceVariable definition = instanceVarDefinitions.get(index);
            return ProgramVariable.create(
                    definition.getOwner(),
                    definition.getName(),
                    definition.getDescriptor(),
                    definition.getInstructionIndex(),
                    definition.getLineNumber());
        }

//        for (Field element : pData.getFields()) {
//            if (element.getName().equals(pName)) {
//                return ProgramVariable.create(
//                        element.getOwner(),
//                        element.getName(),
//                        element.getDescriptor(),
//                        Integer.MIN_VALUE,
//                        Integer.MIN_VALUE);
//            }
//        }
        return null;
    }

    private List<ProgramVariable> getPossibleVars(final ClassExecutionData pData,
                                                  final int pLineNumber,
                                                  final String pName) {
        List<ProgramVariable> result = new ArrayList<>();
        for (Map.Entry<String, Set<ProgramVariable>> entry : pData.getVariablesCovered().entrySet()) {
            for (ProgramVariable element : entry.getValue()) {
                if (element.getLineNumber() == pLineNumber
                        && element.getName().equals(pName)
                        && !result.contains(element)) {
                    result.add(element);
                }
            }
            for (ProgramVariable element : pData.getVariablesUncovered().get(entry.getKey())) {
                if (element.getLineNumber() == pLineNumber
                        && element.getName().equals(pName)
                        && !result.contains(element)) {
                    result.add(element);
                }
            }
        }
        return result;
    }

    private List<InstanceVariable> getPossibleInstanceVarDefinitions(final Set<InstanceVariable> pInstanceVariables,
                                                                     final int pLineNumber,
                                                                     final String pName) {
        List<InstanceVariable> result = new ArrayList<>();
        for (InstanceVariable element : pInstanceVariables) {
            if (element.getLineNumber() == pLineNumber && element.getName().equals(pName)) {
                result.add(element);
            }
        }
        return result;
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

        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for (DefUsePair defUsePair : defUsePairs.getValue()) {
                ProgramVariable definition = defUsePair.getDefinition();
                if (definition.getLineNumber() > pLineNumber
                        && definition.getName().equals(pName)
                        && pData.getMethodFirstLine().get(defUsePairs.getKey()) <= pLineNumber
                        && pData.getMethodLastLine().get(defUsePairs.getKey()) >= pLineNumber
                        && !pData.isAnalyzedDefinition(pName, pLineNumber)) {
                    return true;
                }
            }
        }
        return false;
    }

//    private boolean isInstanceVariable(ClassExecutionData pData, int pLineNumber, String pName) {
//        for (Field field : pData.getFields()) {
//            if (field.getName().equals(pName) && field.getLineNumber() > pLineNumber) {
//                return true;
//            }
//        }
//        return false;
//    }

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

    private boolean isVarCovered(ClassExecutionData pData, ProgramVariable pVariable) {
        for (Map.Entry<String, Set<ProgramVariable>> variablesCovered : pData.getVariablesCovered().entrySet()) {
            if (variablesCovered.getValue().contains(pVariable)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDefinition(ProgramVariable pVariable, Set<DefUsePair> pDefUsePairs) {
        for (DefUsePair defUsePair : pDefUsePairs) {
            if (defUsePair.getDefinition().equals(pVariable)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsage(ProgramVariable pVariable, Set<DefUsePair> pDefUsePairs) {
        for (DefUsePair defUsePair : pDefUsePairs) {
            if (defUsePair.getUsage().equals(pVariable)) {
                return true;
            }
        }
        return false;
    }

    // find all uses for one particular definition
    private Map<DefUsePair, Boolean> getDefUsePairsCoveredForVar(ClassExecutionData pData, ProgramVariable pVariable) {
        Map<DefUsePair, Boolean> result = new HashMap<>();
        for (Map<DefUsePair, Boolean> duCoveredMap : pData.getDefUsePairsCovered().values()) {
            for (Map.Entry<DefUsePair, Boolean> element : duCoveredMap.entrySet()) {
                if (element.getKey().getDefinition().equals(pVariable) || element.getKey().getUsage().equals(pVariable)) {
                    result.put(element.getKey(), element.getValue());
                }
            }
        }
        return result;
    }

    private boolean isVariableCovered(ClassExecutionData pData, ProgramVariable pVariable) {
        for (Map.Entry<String, Set<ProgramVariable>> entry : pData.getVariablesCovered().entrySet()) {
            if (entry.getValue().contains(pVariable)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllDefsCovered(ClassExecutionData pData, ProgramVariable pUsage) {
        boolean allDefsCovered = true;
        for (Map.Entry<String, Set<ProgramVariable>> entry : pData.getVariablesCovered().entrySet()) {
            if (entry.getValue().contains(pUsage)) {
                for (DefUsePair pair : pData.getDefUsePairs().get(entry.getKey())) {
                    if (pair.getUsage().equals(pUsage)) {
                        allDefsCovered = allDefsCovered && entry.getValue().contains(pair.getDefinition());
                    }
                }
            }
        }
        return allDefsCovered;
    }
}
