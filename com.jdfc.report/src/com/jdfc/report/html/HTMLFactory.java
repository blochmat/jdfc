package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.data.InterProceduralMatch;
import com.jdfc.core.analysis.ifg.data.DefUsePair;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.report.html.resources.Resources;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The {@code HTMLFactory} creates all files contained in the jdfc-report directory.
 */

public class HTMLFactory {

    private final Resources resources;
    private final File baseDir;

    /**
     * Constructor of {@code HTMLFactory}
     *
     * @param pResources information about resources required for html files
     * @param pBaseDir root output directory
     */
    public HTMLFactory(Resources pResources, File pBaseDir) {
        baseDir = pBaseDir;
        resources = pResources;
        try {
            resources.copyResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Java reserved keywords
     */
    final String[] JAVA_KEYWORDS = {"abstract", "assert",
            "break", "case", "catch", "class", "const",
            "continue", "default", "do", "else", "extends", "false",
            "final", "finally", "for", "goto", "if", "implements",
            "import", "instanceof", "interface", "native",
            "new", "null", "package", "private", "protected", "public",
            "return", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while"};

    /**
     * Java type keywords
     */
    final String[] TYPE_KEYWORDS = {"boolean", "byte", "char", "double", "float", "int", "long", "short"};

    /**
     * Stylesheet file name
     */
    final String STYLE_SHEET = "report.css";

    /**
     * Script file name
     */
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

            HTMLElement finalizedText = finalizeLineText(pClassFile, pClassName, currentLineCounter, currentLineString, pData);
            sourceCodeText.getContent().add(finalizedText);
        }
        scanner.close();
        return table;
    }

    private HTMLElement finalizeLineText(final File pClassFile,
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
            if (item.contains("*") || item.contains("/**")) {
                divTagLine.getContent().add(HTMLElement.noTag(String.join("", workList)));
                return divTagLine;
            }
            int sameWordsCount = (int) workList.stream().filter(x -> x.equals(item)).count();
            workList.remove(0);
            
            Pattern specCharPattern = Pattern.compile("\\W+");
            Matcher specCharMatcher = specCharPattern.matcher(item);

            if (specCharMatcher.matches()) {
                if (item.contains("//")) {
                    divTagLine.getContent().add(createCommentSpan(item, workList));
                } else if (item.contains("\"") || item.contains("'")) {
                    processString(divTagLine, workList, item);
                } else {
                    divTagLine.getContent().add(HTMLElement.pre(item));
                }
            } else {
                String methodName = pData.getMethodNameFromLineNumber(pLineNumber);
                ProgramVariable programVariable = findProgramVariable(pData, methodName, pLineNumber, item, sameWordsCount);

                if (programVariable == null) {
                    HTMLElement spanTag = HTMLElement.span(item);
                    divTagLine.getContent().add(spanTag);
                    addCodeHighlighting(spanTag, item);
                } else {
                    if (isRedefined(pData, pLineNumber, item)) {
                        HTMLElement spanTag = HTMLElement.span(item);
                        divTagLine.getContent().add(spanTag);
                        spanTag.getAttributes().add("class=\"orange\"");
                    } else {
                        boolean isVarCovered = isVarCovered(pData, programVariable);

                            List<ProgramVariable> associatedVars = getAssociatedVars(programVariable, pData);
                            Map<DefUsePair, Boolean> defUsePairsCovered = getDefUsePairCovered(pData, associatedVars);
                            String backgroundColor = getVariableBackgroundColor(isVarCovered, defUsePairsCovered);
                            divTagLine.getContent().add(
                                    createVariableInformation(pData, pClassFile, pClassName,
                                            defUsePairsCovered, programVariable, backgroundColor));
                    }
                }
            }
        }
        return divTagLine;
    }

    private void processString(HTMLElement pDivTagLine, List<String> pWorkList, String pItem) {
//        System.out.println("found");
        List<String> stringContent = new ArrayList<>();
        List<String> arr = Arrays.asList(pItem.split(""));
        int singleQuoteIndex = arr.indexOf("'");
        int doubleQuoteIndex = arr.indexOf("\"");
        boolean isSingleQuoted = false;

        if (singleQuoteIndex != -1) {
            isSingleQuoted = singleQuoteIndex < doubleQuoteIndex;
        }

        String comparator;
        int index;
        if (isSingleQuoted) {
            comparator = "'";
            index = singleQuoteIndex;
        } else {
            comparator = "\"";
            index = doubleQuoteIndex;
        }

        final String finalComparator = comparator;
        final int finalIndex = index;

        boolean isClosed = isClosed(finalComparator, arr);
        if (!isClosed) {
            String beforeStart = pItem.substring(finalIndex);
            pDivTagLine.getContent().add(HTMLElement.noTag(beforeStart));
            stringContent.add(finalComparator);
            String afterStart = pItem.substring(finalIndex+1);
            stringContent.add(afterStart);
            while (!pWorkList.isEmpty() && !pWorkList.get(0).contains(comparator)) {
                stringContent.add(pWorkList.get(0));
                pWorkList.remove(0);
            }
            if(!pWorkList.isEmpty()) {
                String closing = pWorkList.get(0);
                pWorkList.remove(0);
                List<String> closingArr = Arrays.asList(closing.split(""));
                int quoteIndex = closingArr.indexOf(finalComparator);
                String beforeEnd = closing.substring(quoteIndex);
                stringContent.add(beforeEnd);
                String afterEnd = closing.substring(quoteIndex+1);
                pWorkList.add(0, afterEnd);
                pDivTagLine.getContent().add(createStringSpan(stringContent));
            }
        }
        pDivTagLine.getContent().add(createStringSpan(stringContent));
    }

    private boolean isClosed(String pFinalComparator, List<String> pString) {
        int count = 0;
        for (int i = 0; i < pString.size(); i++) {
            if (pString.get(i).equals("\\")
                    && i < pString.size() - 1
                    && pString.get(i - 1).equals(pFinalComparator)) {
                count--;
            }
            if (pString.get(i).equals(pFinalComparator)) {
                count++;
            }
        }
        return count % 2 == 0;
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
            if (!workListSpec.isEmpty() && workListSpec.get(0).contains("//") && workListWords.size() < pWords.size()) {
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
                if(!workListWords.isEmpty()) {
                    result.add(workListWords.get(0));
                    workListWords.remove(0);
                }

                if (!workListSpec.isEmpty()) {
                    result.add(workListSpec.get(0));
                    workListSpec.remove(0);
                }
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

    private HTMLElement createStringSpan(List<String> pStringContent) {
        HTMLElement string = HTMLElement.pre();
        string.getAttributes().add("class=\"string\"");
        while (!pStringContent.isEmpty()) {
            string.getContent().add(HTMLElement.noTag(pStringContent.get(0)));
            pStringContent.remove(0);
        }
        return string;
    }

    private List<ProgramVariable> getAssociatedVars(ProgramVariable pVariable, ClassExecutionData pData) {
        List<ProgramVariable> result = new ArrayList<>();
        result.add(pVariable);
        for (InterProceduralMatch entry : pData.getInterProceduralMatches()) {
            if (entry.getDefinition().equals(pVariable)) {
                result.add(entry.getCallSiteDefinition());
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
                                                  final ProgramVariable pProgramVariable,
                                                  final String pBackgroundColor) {
        HTMLElement variableSpan = createVariableSpan(pProgramVariable, pBackgroundColor);
        HTMLElement tooltipContent;
        tooltipContent = createVariableTooltipContent(pData, pClassFile, pClassName, pDefUseCovered, pProgramVariable);
        HTMLElement tooltipDiv = createTooltipDiv();
        tooltipDiv.getContent().add(tooltipContent);
        HTMLElement variableDiv = createVariableDiv();
        variableDiv.getContent().add(variableSpan);
        variableDiv.getContent().add(tooltipDiv);
        return variableDiv;
    }

    private HTMLElement createVariableSpan(final ProgramVariable pVariable,
                                           final String pBackgroundColor) {
        String variableString = pVariable.getName();
        HTMLElement variableSpan = HTMLElement.span(variableString);
        String variableSpanAttr = String.format("class=\"link %s\"", pBackgroundColor);
        variableSpan.getAttributes().add(variableSpanAttr);
        return variableSpan;
    }

    private HTMLElement createVariableTooltipContent(final ClassExecutionData pData,
                                                     final File pClassFile,
                                                     final String pClassName,
                                                     final Map<DefUsePair, Boolean> pDefUseCovered,
                                                     final ProgramVariable pProgramVariable) {
        boolean isDefinition = isDefinition(pProgramVariable, pDefUseCovered.keySet());
        boolean isUsage = isUsage(pProgramVariable, pDefUseCovered.keySet());

        HTMLElement table = HTMLElement.table();
        table.getAttributes().add("class=\"white-gray rounded-corners\"");
        table.getContent().add(createTabButtons(pProgramVariable, isDefinition, isUsage));

        if (isDefinition) {
            Set<ProgramVariable> correlatedDefs = extractCorrelatedDefs(pDefUseCovered, pProgramVariable);
            if (!correlatedDefs.isEmpty()) {
                table.getContent().add(createAssociatedVariablesSpan(pProgramVariable));
                table.getContent().add(createAssociatedVariablesTable(pData, pClassFile, pClassName, pProgramVariable, correlatedDefs));
            }
            table.getContent().add(createUsagesSpan(pProgramVariable));
            table.getContent().add(createTabInfoTable(pClassFile, pClassName, pProgramVariable, pDefUseCovered, true));
        }

        if (isUsage) {
            table.getContent().add(createDefinitionsSpan(pProgramVariable));
            table.getContent().add(createTabInfoTable(pClassFile, pClassName, pProgramVariable, pDefUseCovered, false));
        }

        return table;
    }

    private HTMLElement createTooltipDiv() {
        HTMLElement tooltipDiv = HTMLElement.div();
        tooltipDiv.getAttributes().add("class=\"tooltipDef dark-gray rounded-corners\"");
        return tooltipDiv;
    }

    private HTMLElement createVariableDiv() {
        HTMLElement variableDiv = HTMLElement.div();
        variableDiv.getAttributes().add("class=\"variable\"");
        return variableDiv;
    }

    private HTMLElement createTabInfoTable(final File pClassFile,
                                           final String pClassName,
                                           final ProgramVariable pVariable,
                                           final Map<DefUsePair, Boolean> pDefUseCovered,
                                           final boolean isDefinitionTab) {
        HTMLElement row = HTMLElement.tr();
        HTMLElement cell = HTMLElement.td();
        row.getContent().add(cell);
        HTMLElement div = HTMLElement.div();
        cell.getContent().add(div);
        HTMLElement table = HTMLElement.table();
        div.getContent().add(table);
        table.getAttributes().add("class=\"tooltipTable gray rounded-corners\"");
        table.getContent().add(createHeaderRow());

        if (isDefinitionTab) {
            String defTab = String.format("class=\"%s%s%sDefTab margin10 green\"", pVariable.getName(),
                    pVariable.getLineNumber(), pVariable.getInstructionIndex());
            div.getAttributes().add(defTab);
        } else {
            String useTab = String.format("class=\"%s%s%sUseTab margin10 green\"", pVariable.getName(),
                    pVariable.getLineNumber(), pVariable.getInstructionIndex());
            div.getAttributes().add(useTab);
        }

        Map<ProgramVariable, Boolean> variableCovered = getCoverageInformation(pDefUseCovered, isDefinitionTab);
        for (Map.Entry<ProgramVariable, Boolean> element : variableCovered.entrySet()) {
            try {
                table.getContent().add(createDataRow(pClassFile, pClassName, element.getKey(), element.getValue()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return row;
    }

    private Map<ProgramVariable, Boolean> getCoverageInformation(final Map<DefUsePair, Boolean> pVariableInfo,
                                                                 final boolean isDefinitionTab) {
        Map<ProgramVariable, Boolean> result = new TreeMap<>();
        for (Map.Entry<DefUsePair, Boolean> entry : pVariableInfo.entrySet()) {
            if (isDefinitionTab) {
                ProgramVariable usage = entry.getKey().getUsage();
                result.put(usage, entry.getValue());
            } else {
                ProgramVariable definition = entry.getKey().getDefinition();
                result.put(definition, entry.getValue());
            }
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
        div.getAttributes().add("class=\"buttonContainer\"");


        HTMLElement definitionButton = null;
        if (isDefinition) {
            definitionButton = HTMLElement.button("Definition");
            String id = String.format("id=\"%s%s%sDefTabButton\"", pVariable.getName(), pVariable.getLineNumber(), pVariable.getInstructionIndex());
            definitionButton.getAttributes().add(id);
            definitionButton.getAttributes().add("onclick=\"openTab(this)\"");
            definitionButton.getAttributes().add("class=\"defaultOpen light-gray\"");
            div.getContent().add(definitionButton);
        }

        HTMLElement usageButton;
        if (isUsage) {
            usageButton = HTMLElement.button("Usage");
            String id = String.format("id=\"%s%s%sUseTabButton\"", pVariable.getName(), pVariable.getLineNumber(), pVariable.getInstructionIndex());
            usageButton.getAttributes().add(id);
            usageButton.getAttributes().add("onclick=\"openTab(this)\"");
            if (definitionButton == null) {
                usageButton.getAttributes().add("class=\"defaultOpen light-gray\"");
            } else {
                usageButton.getAttributes().add("class=\"light-gray\"");
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

    private HTMLElement createAssociatedVariablesSpan(final ProgramVariable pVariable) {
        HTMLElement row = HTMLElement.tr();
        HTMLElement cell = HTMLElement.td();
        row.getContent().add(cell);

        HTMLElement div = HTMLElement.div();
        String defTab = String.format("class=\"%s%s%sDefTab\"", pVariable.getName(),
                pVariable.getLineNumber(), pVariable.getInstructionIndex());
        div.getAttributes().add(defTab);
        cell.getContent().add(div);

        HTMLElement span = HTMLElement.span("Associated Definitions:");
        span.getAttributes().add("class=\"margin10 nowrap\"");
        div.getContent().add(span);
        return row;
    }

    private HTMLElement createUsagesSpan(final ProgramVariable pVariable) {
        String defTab = String.format("class=\"%s%s%sDefTab\"", pVariable.getName(),
                pVariable.getLineNumber(), pVariable.getInstructionIndex());
        HTMLElement span = HTMLElement.span("Usages:");
        span.getAttributes().add("class=\"margin10 nowrap\"");
        return createRowStructure(defTab, span);
    }

    private HTMLElement createDefinitionsSpan(final ProgramVariable pVariable) {
        String defTab = String.format("class=\"%s%s%sUseTab\"", pVariable.getName(),
                pVariable.getLineNumber(), pVariable.getInstructionIndex());
        HTMLElement span = HTMLElement.span("Definitions:");
        span.getAttributes().add("class=\"margin10 nowrap\"");
        return createRowStructure(defTab, span);
    }

    private HTMLElement createAssociatedVariablesTable(final ClassExecutionData pData,
                                                       final File pClassFile,
                                                       final String pClassName,
                                                       final ProgramVariable pProgramVariable,
                                                       final Set<ProgramVariable> pCorrelatedDefs) {
        String defTab = String.format("class=\"%s%s%sDefTab\"", pProgramVariable.getName(),
                pProgramVariable.getLineNumber(), pProgramVariable.getInstructionIndex());

        HTMLElement table = HTMLElement.table();
        table.getAttributes().add("class=\"margin10 gray rounded-corners\"");
        table.getContent().add(createHeaderRow());
        for (ProgramVariable element : pCorrelatedDefs) {
            boolean covered = isVarCovered(pData, pProgramVariable);
            try {
                table.getContent().add(createDataRow(pClassFile, pClassName, element, covered));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return createRowStructure(defTab, table);
    }

    private HTMLElement createRowStructure(String defTab, HTMLElement element) {
        HTMLElement div = HTMLElement.div();
        div.getAttributes().add(defTab);
        div.getContent().add(element);
        HTMLElement cell = HTMLElement.td();
        cell.getContent().add(div);
        HTMLElement row = HTMLElement.tr();
        row.getContent().add(cell);
        return row;
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
        return Arrays.asList(JAVA_KEYWORDS).contains(pWord);
    }

    private boolean isTypeKeyword(String pWord) {
        return Arrays.asList(TYPE_KEYWORDS).contains(pWord);
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
    private ProgramVariable findProgramVariable(final ClassExecutionData pData,
                                                final String pMethodName,
                                                final int pLineNumber,
                                                final String pName,
                                                final int pSameWordsCount) {
        if (pMethodName != null) {
            List<ProgramVariable> possibleVariables = getPossibleVars(pData, pMethodName, pLineNumber, pName);
            if (!possibleVariables.isEmpty()) {
                possibleVariables.sort(Comparator.comparing(ProgramVariable::getInstructionIndex));
                int index = possibleVariables.size() - pSameWordsCount;
                // TODO: B0002 - Multiple words (Method calls, variables, instanceVariables) can not be distinguished
                if (index < 0) {
                    return null;
                }
                return possibleVariables.get(index);
            }
        } else {
            return getMethodParamDefinition(pData, pName);
        }
        return null;
    }

    private List<ProgramVariable> getPossibleVars(final ClassExecutionData pData,
                                                  final String pMethodName,
                                                  final int pLineNumber,
                                                  final String pName) {
        List<ProgramVariable> result = new ArrayList<>();
        if(pData.getVariablesCovered().get(pMethodName) != null) {
            for (ProgramVariable element : pData.getVariablesCovered().get(pMethodName)) {
                if (element.getLineNumber() == pLineNumber
                        && element.getName().equals(pName)
                        && !result.contains(element)) {
                    result.add(element);
                }
            }
        }

        if(pData.getVariablesUncovered().get(pMethodName) != null) {
            for (ProgramVariable element : pData.getVariablesUncovered().get(pMethodName)) {
                if (element.getLineNumber() == pLineNumber
                        && element.getName().equals(pName)
                        && !result.contains(element)) {
                    result.add(element);
                }
            }
        }

        return result;
    }

    private ProgramVariable getMethodParamDefinition(final ClassExecutionData pData,
                                                     final String pName) {
        for (Map.Entry<String, Set<ProgramVariable>> entry : pData.getVariablesCovered().entrySet()) {
            for (ProgramVariable element : entry.getValue()) {
                if (element.getLineNumber() == Integer.MIN_VALUE
                        && element.getName().equals(pName)) {
                    return element;
                }
            }
            for (ProgramVariable element : entry.getValue()) {
                if (element.getLineNumber() == Integer.MIN_VALUE
                        && element.getName().equals(pName)) {
                    return element;
                }
            }
        }
        return null;
    }

    private boolean isRedefined(ClassExecutionData pData, int pLineNumber, String pName) {
        for (Map.Entry<String, List<DefUsePair>> defUsePairs : pData.getDefUsePairs().entrySet()) {
            for (DefUsePair defUsePair : defUsePairs.getValue()) {
                ProgramVariable definition = defUsePair.getDefinition();
                if (definition.getLineNumber() > pLineNumber
                        && definition.getName().equals(pName)
                        && pData.getMethodFirstLine().get(defUsePairs.getKey()) <= pLineNumber
                        && pData.getMethodLastLine().get(defUsePairs.getKey()) >= pLineNumber
                        && !pData.isAnalyzedVariable(pName, pLineNumber)) {
                    return true;
                }
            }
        }
        return false;
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
}
