package report.html;

import data.*;
import data.singleton.CoverageDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The {@code HTMLFactory} creates all files contained in the jdfc-report directory.
 */

public class HTMLFactory {

    private Logger logger = LoggerFactory.getLogger(HTMLFactory.class);
    private final Resources resources;
    private final File baseDir;

    private boolean isJavaDoc;

    /**
     * Constructor of {@code HTMLFactory}
     *
     * @param pResources information about resources required for html files
     * @param pBaseDir   root output directory
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

    public void createPkgIndexHTML(File pkg, Map<String, ClassData> classDataMap) throws IOException {
        String indexPath = String.format("%s/index.html", pkg);
        File index = new File(indexPath);

        String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(index), STYLE_SHEET);
        String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(index), SCRIPT);

        HTMLElement indexHTML = createIndexHTML(classDataMap, pkg, styleSheetPath, scriptPath);

        Writer writer = new FileWriter(index);
        writer.write(indexHTML.render());
        writer.close();
    }

    private HTMLElement createIndexHTML(final Map<String, ClassData> classDataMap,
                                        final File pkg,
                                        final String styleSheetRel,
                                        final String scriptRel) {
        String[] split = pkg.toString().replace(File.separator, "/").split("/");
        String title = split[split.length - 1];

        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(title, styleSheetRel));

        HTMLElement htmlBodyTag = createDefaultHTMLBody(title, pkg, scriptRel, null);
        List<String> columns = new ArrayList<>(Arrays.asList("Method Count", "Covered %", "Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createClassesTable(columns, classDataMap));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    public void createRootIndexHTML(File outputDir) throws IOException {
        String indexPath = String.format("%s/index.html", outputDir);
        File index = new File(indexPath);

        String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(index), STYLE_SHEET);
        String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(index), SCRIPT);

        HTMLElement indexHTML = createRootIndexHTML(outputDir, styleSheetPath, scriptPath);

        Writer writer = new FileWriter(index);
        writer.write(indexHTML.render());
        writer.close();
    }

    private HTMLElement createRootIndexHTML(File outputDir, String styleSheetRel, String scriptRel) {
        String[] split = outputDir.toString().replace(File.separator, "/").split("/");
        String title = split[split.length - 1];

        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(title, styleSheetRel));

        HTMLElement htmlBodyTag = createDefaultHTMLBody(title, outputDir, scriptRel, null);
        List<String> columns = new ArrayList<>(Arrays.asList("Method Count", "Covered %", "Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createPackagesTable(columns));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    public void createClassOverviewHTML(final String pClassName,
                                        final ExecutionData pData,
                                        final File pWorkDir) throws IOException {
        logger.debug(String.format("createClassOverviewHTML(%s, <ExecutionData>, %s)", pClassName, pWorkDir));
        if (pData instanceof ClassData) {
            String filePath = String.format("%s/%s.html", pWorkDir, pClassName);
            File classFile = new File(filePath);

            String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(classFile), STYLE_SHEET);
            String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(classFile), SCRIPT);

            HTMLElement classOverviewHTML =
                    createClassOverviewHTML((ClassData) pData, classFile, pClassName, styleSheetPath, scriptPath);

            Writer writer = new FileWriter(classFile);
            writer.write(classOverviewHTML.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from ExecutionData instance.");
        }
    }

    private HTMLElement createClassOverviewHTML(final ClassData pData,
                                                final File pClassFile,
                                                final String pClassFileName,
                                                final String pPathToStyleSheet,
                                                final String pPathToScript) {
        logger.debug(String.format("createClassOverviewHTML(<ClassExecutionData>, %s, %s, %s, %s)", pClassFile, pClassFileName, pPathToStyleSheet, pPathToScript));
        String classFileName = String.format("%s.java", pClassFileName);

        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, pPathToStyleSheet));

        HTMLElement htmlBodyTag = createDefaultHTMLBody(pClassFileName, pClassFile, pPathToScript, null);
        List<String> columns = new ArrayList<>(Arrays.asList("Total", "Covered", "Missed"));
        htmlBodyTag.getContent().add(createMethodsTable(columns, pData, pClassFileName));
        htmlMainTag.getContent().add(htmlBodyTag);
        return htmlMainTag;
    }

    private HTMLElement createBreadcrumbs(File pWorkDir) {
        List<String> split =
                new ArrayList<>(Arrays.asList(pWorkDir.toPath().relativize(baseDir.toPath()).toString().replace(File.separator, "/").split("/")));
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

            String link = String.format("%s%sindex.html", String.join(File.separator, split), File.separator);
            breadcrumbs.getContent().add(HTMLElement.a(link, parent.getName()));
            breadcrumbs.getContent().add(HTMLElement.noTag(" > "));
            split.remove(0);
        }
        breadcrumbs.getContent().add(HTMLElement.span(pWorkDir.getName()));
        return breadcrumbs;
    }

    public void createClassSourceViewHTML(final String pClassName,
                                          final ExecutionData pData,
                                          final File pWorkDir,
                                          final File pSourceDir)
            throws IOException {
        logger.debug(String.format("createClassSourceViewHTML(%s, <ExecutionData>, %s, %s)", pClassName, pWorkDir.toString(), pSourceDir.toString()));
        if (pData instanceof ClassData) {
            String sourceViewPath = String.format("%s/%s.java.html", pWorkDir, pClassName);
            File sourceViewHTML = new File(sourceViewPath);

            String styleSheetPath = String.format("%s/%s", resources.getPathToResourcesFrom(sourceViewHTML), STYLE_SHEET);
            String scriptPath = String.format("%s/%s", resources.getPathToResourcesFrom(sourceViewHTML), SCRIPT);

            // load class file
            String classFilePath = String.format("%s/%s.java", pSourceDir, ((ClassData) pData).getRelativePath());
            File classFile = new File(classFilePath);

            // build html
            HTMLElement classSourceViewHTML =
                    createClassSourceViewHTML(classFile, (ClassData) pData, pClassName, styleSheetPath, scriptPath);

            // save file
            Writer writer = new FileWriter(sourceViewHTML);
            writer.write(classSourceViewHTML.render());
            writer.close();
        } else {
            throw new IllegalArgumentException("Class Overview can not be created from ExecutionData instance.");
        }
    }

    private HTMLElement createClassSourceViewHTML(final File pClassFile,
                                                  final ClassData pData,
                                                  final String pClassName,
                                                  final String pPathToStyleSheet,
                                                  final String pPathToScript) {
        logger.debug(String.format("createClassSourceViewHTML(%s, <ClassExecutionData>, %s, %s, %s)", pClassFile, pClassName, pPathToStyleSheet, pPathToScript));
        // Standard html file creation, nothing special
        String classFileName = String.format("%s.java", pClassName);
        HTMLElement htmlMainTag = HTMLElement.html();
        htmlMainTag.getContent().add(createDefaultHTMLHead(classFileName, pPathToStyleSheet));

        HTMLElement htmlBodyTag = createDefaultHTMLBody(pClassName, null, pPathToScript, "code");
        htmlMainTag.getContent().add(htmlBodyTag);
        try {
            // Fill body
            htmlBodyTag.getContent().add(createSourceCode(pClassFile, pData, pClassName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return htmlMainTag;
    }

    private HTMLElement createSourceCode(final File pClassFile,
                                         final ClassData pData,
                                         final String pClassName) throws FileNotFoundException {
        logger.debug(String.format("createSourceCode(%s, <ClassExecutionData>, %s)", pClassFile, pClassName));
        // Read source file
        Scanner scanner = new Scanner(pClassFile);

        // Get all method definitions from the ast

        HTMLElement table = HTMLElement.table();
        table.getAttributes().add("id=\"classDetailView\"");
        int currentLineCounter = 0;

        // for each line
        while (scanner.hasNextLine()) {
            currentLineCounter += 1;
            String currentLineString = scanner.nextLine();

            HTMLElement row = HTMLElement.tr();
            table.getContent().add(row);

            // First cell in row: Line number
            HTMLElement sourceCodeLine = HTMLElement.td(currentLineCounter);
            sourceCodeLine.getAttributes().add(String.format("id=\"L%s\"", currentLineCounter));
            row.getContent().add(sourceCodeLine);

            // Second cell in row: class file text
            HTMLElement sourceCodeText = HTMLElement.td();
            row.getContent().add(sourceCodeText);
            // Add highlighting, coverage info etc.
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
                                         final ClassData pData) {
        logger.debug(String.format("finalizeLineText(%s, %s, %s, %s, <ClassExecutionData>)", pClassFile, pClassName, pLineNumber, pLineString));
        HTMLElement divTagLine = HTMLElement.div();
        divTagLine.getAttributes().add("class=\"line\"");

        List<String> specialChars = extractChars(pLineString, "\\w+\\b");
        List<String> words = extractChars(pLineString, "\\W+");
        List<String> workList = createWorkList(words, specialChars);

        // workList contains words and special chars
        while (!workList.isEmpty()) {
            String item = workList.get(0);
            if (this.isJavaDoc(item)) {
                divTagLine.getContent().add(HTMLElement.noTag(String.join("", workList)));
                return divTagLine;
            }
            int sameWordsCount = (int) workList.stream().filter(x -> x.equals(item)).count();
            workList.remove(0);

            Pattern specCharPattern = Pattern.compile("\\W+");
            Matcher specCharMatcher = specCharPattern.matcher(item);

            // if letter is a special char
            if (specCharMatcher.matches()) {
                // comments
                if (item.contains("//")) {
                    divTagLine.getContent().add(createCommentSpan(item, workList));
                // string
                } else if (item.contains("\"") || item.contains("'")) {
                    processString(divTagLine, workList, item);
                // everything else?
                } else {
                    divTagLine.getContent().add(HTMLElement.pre(item));
                }
            } else {
                // normal text
                // search for method by line number
//                String methodName = pData.getInternalMethodNameByLine(pLineNumber, mDeclList);
                // what we want
                MethodData mData = pData.getMethodByLineNumber(pLineNumber);
                String methodName = null;
                ProgramVariable programVariable = null;
                if (mData != null) {
                    methodName = mData.buildInternalMethodName();
                    boolean isInlineDefinition = false;
                    if (!workList.isEmpty()) {
                        isInlineDefinition = this.isDefinition(workList.get(0));
                    }
                    boolean isMethodParam = isMethodParam(mData, pLineString);
                    boolean isDefinition = isInlineDefinition || isMethodParam;


                    // TODO: Here we are searching through covered and uncovered vars (1)
                    if(isMethodParam) {
                        programVariable = findProgramVariable(mData, Integer.MIN_VALUE, item,true);
                    } else {
                        programVariable = findProgramVariable(mData, pLineNumber, item, isDefinition);
                    }
//                    // check if variable is a param
//                    MethodDeclaration mDecl = mData.getSrcAst();
//                    Optional<Parameter> paramOpt = mDecl.getParameterByName(item);
//                    if (paramOpt.isPresent()) {
//                        Parameter param = paramOpt.get();
//                        Optional<Range> rangeOpt = param.getRange();
//                        if(rangeOpt.isPresent()) {
//                            Range range = rangeOpt.get();
//                            if (range.begin.line == pLineNumber) {
//                                // TODO: Params seems to be empty or params are not found properly yet.
//                                programVariable = mData.findParamByName(item);
//                            }
//                        }
//                    }
                }

                // search for variable in method
                if (programVariable == null) {
                    // if word is no variable
                    HTMLElement spanTag = HTMLElement.span(item);
                    divTagLine.getContent().add(spanTag);
                    // add highlighting if it is a special word
                    addCodeHighlighting(spanTag, item);
                } else {
                    if (isRedefined(mData, pLineNumber, item)) {
                        // is redefined variable and therefore no longer present in the coverage data
                        HTMLElement spanTag = HTMLElement.span(item);
                        divTagLine.getContent().add(spanTag);
                        spanTag.getAttributes().add("class=\"orange\"");
                    } else {
                        // is a variable present in the data

                        // check if associated uses or defs are covered and highlight
                        Set<DefUsePair> pairSet = getDefUsePairsCoveredForVar(mData, programVariable);
                        String backgroundColor = getVariableBackgroundColor(programVariable.getIsCovered(), pairSet);
                        divTagLine.getContent().add(
                                createVariableInformation(pClassFile, pClassName, pairSet, programVariable, backgroundColor));
                    }
                }
            }
        }
        return divTagLine;
    }

    private boolean isJavaDoc(String item) {
        if (item.contains("/**")) {
            this.isJavaDoc = true;
        }

        if (item.contains("*/")) {
            this.isJavaDoc = false;
        }

        return this.isJavaDoc;
    }

    private boolean isDefinition(String topOfWorkList) {
        boolean isAssignment = topOfWorkList.contains("=");
        boolean isComparison = topOfWorkList.contains("==") || topOfWorkList.contains("!=") || topOfWorkList.contains("<=") || topOfWorkList.contains(">=");

        return (isAssignment && !isComparison);
    }

    private boolean isMethodParam(MethodData mData, String lineString) {
        return mData.getDeclarationStr().contains(lineString.replace("{","").trim());
    }

    private void processString(HTMLElement pDivTagLine, List<String> pWorkList, String pItem) {
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
            String afterStart = pItem.substring(finalIndex + 1);
            stringContent.add(afterStart);
            while (!pWorkList.isEmpty() && !pWorkList.get(0).contains(comparator)) {
                stringContent.add(pWorkList.get(0));
                pWorkList.remove(0);
            }
            if (!pWorkList.isEmpty()) {
                String closing = pWorkList.get(0);
                pWorkList.remove(0);
                List<String> closingArr = Arrays.asList(closing.split(""));
                int quoteIndex = closingArr.indexOf(finalComparator);
                String beforeEnd = closing.substring(quoteIndex);
                stringContent.add(beforeEnd);
                String afterEnd = closing.substring(quoteIndex + 1);
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
                if (!workListWords.isEmpty()) {
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

    private HTMLElement createPackagesTable(final List<String> pColumns) {
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getContent().add(createTableHeadTag(pColumns));
        tableTag.getContent().add(createPackagesTableBodyTag());
        tableTag.getContent().add(createPackagesTableFootTag());
        return tableTag;
    }

    private HTMLElement createClassesTable(final List<String> pColumns, Map<String, ClassData> classDataMap) {
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getContent().add(createTableHeadTag(pColumns));
        tableTag.getContent().add(createClassesTableBodyTag(classDataMap));

        // TODO: Build the sum of all displayed elements
        tableTag.getContent().add(createClassesTableFootTag(classDataMap));
        return tableTag;
    }

    private HTMLElement createMethodsTable(final List<String> pColumns,
                                           final ClassData pData,
                                           final String pClassfileName) {
        logger.debug(String.format("createClassesTable(%s, <ExecutionData>, %s)", pColumns.toString(), pClassfileName));
        HTMLElement tableTag = HTMLElement.table();
        tableTag.getContent().add(createTableHeadTag(pColumns));
        for (MethodData mData : pData.getMethods().values()) {
            String internalMethodName = mData.buildInternalMethodName();

            // Methods with 0 DefUsePairs are standard object constructors; we do not want to show those.
            if (mData.getPairs().size() != 0) {
                if (internalMethodName.contains("<init>")) {
                    internalMethodName = internalMethodName.replace("<init>", "init");
                }
                int total = mData.getTotal();
                int covered = mData.getCovered();
                int missed = total - covered;
                String link = String.format("%s.java.html#L%s", pClassfileName, mData.getBeginLine());
                HTMLElement trTag = HTMLElement.tr();
                HTMLElement tdTag = HTMLElement.td();
                tdTag.getContent().add(HTMLElement.a(link, internalMethodName));
                trTag.getContent().add(tdTag);
                trTag.getContent().add(HTMLElement.td(total));
                trTag.getContent().add(HTMLElement.td(covered));
                trTag.getContent().add(HTMLElement.td(missed));
                tableTag.getContent().add(trTag);
            }
        }
        tableTag.getContent().add(createMethodsTableFootTag(pData));
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

    private HTMLElement createPackagesTableBodyTag() {
        HTMLElement bodyTag = HTMLElement.tbody();
        for (Map.Entry<String, Map<String, ClassData>> entry : CoverageDataStore.getInstance().getProjectData().entrySet()) {
            HTMLElement trTag = HTMLElement.tr();

            // First link tag
            HTMLElement tdTag = HTMLElement.td();
            tdTag.getContent().add(HTMLElement.a(entry.getKey(), entry.getKey()));

            trTag.getContent().add(tdTag);

            // TODO
            int methodCount = 0;
            int pairsTotal = 0;
            int pairsCovered = 0;
            double percentage = 0;
            trTag.getContent().add(HTMLElement.td(methodCount));
            trTag.getContent().add(HTMLElement.td("TODO"));
            trTag.getContent().add(HTMLElement.td(pairsTotal));
            trTag.getContent().add(HTMLElement.td(pairsCovered));
            trTag.getContent().add(HTMLElement.td(percentage));
            bodyTag.getContent().add(trTag);
        }
        return bodyTag;
    }

    private HTMLElement createPackagesTableFootTag() {
        HTMLElement tfootTag = HTMLElement.tfoot();
        HTMLElement rowTag = HTMLElement.tr();
        tfootTag.getContent().add(rowTag);
        // TODO
        int pkgMethodCount = 0;
        int totalPairs = 0;
        int coveredPairs = 0;
        double percentage = 0.00;
        rowTag.getContent().add(HTMLElement.td("Total"));
        rowTag.getContent().add(HTMLElement.td(pkgMethodCount));
        rowTag.getContent().add(HTMLElement.td("TODO"));
        rowTag.getContent().add(HTMLElement.td(totalPairs));
        rowTag.getContent().add(HTMLElement.td(coveredPairs));
        rowTag.getContent().add(HTMLElement.td(percentage));
        return tfootTag;
    }

    private HTMLElement createClassesTableBodyTag(Map<String, ClassData> classDataMap) {
        HTMLElement bodyTag = HTMLElement.tbody();
        for (Map.Entry<String, ClassData> entry : classDataMap.entrySet()) {
            ExecutionData data = entry.getValue();
            HTMLElement trTag = HTMLElement.tr();

            // First link tag
            HTMLElement tdTag = HTMLElement.td();
            String link = String.format("%s.html", entry.getKey());
            tdTag.getContent().add(HTMLElement.a(link, entry.getKey()));

            trTag.getContent().add(tdTag);
            trTag.getContent().add(HTMLElement.td(data.getMethodCount()));
            // TODO: fill with rate
            trTag.getContent().add(HTMLElement.td("TODO"));
            trTag.getContent().add(HTMLElement.td(data.getTotal()));
            trTag.getContent().add(HTMLElement.td(data.getCovered()));
            trTag.getContent().add(HTMLElement.td(data.getTotal() - data.getCovered()));
            bodyTag.getContent().add(trTag);
        }
        return bodyTag;
    }

    private HTMLElement createClassesTableFootTag(final Map<String, ClassData> classDataMap) {
        HTMLElement tfootTag = HTMLElement.tfoot();
        HTMLElement rowTag = HTMLElement.tr();
        tfootTag.getContent().add(rowTag);
        // TODO
        int pkgMethodCount = 0;
        int totalPairs = 0;
        int coveredPairs = 0;
        double percentage = 0.00;
        rowTag.getContent().add(HTMLElement.td("Total"));
        rowTag.getContent().add(HTMLElement.td(pkgMethodCount));
        rowTag.getContent().add(HTMLElement.td("TODO"));
        rowTag.getContent().add(HTMLElement.td(totalPairs));
        rowTag.getContent().add(HTMLElement.td(coveredPairs));
        rowTag.getContent().add(HTMLElement.td(percentage));
        return tfootTag;
    }

    private HTMLElement createMethodsTableFootTag(final ClassData pData) {
        HTMLElement tfootTag = HTMLElement.tfoot();
        HTMLElement rowTag = HTMLElement.tr();
        tfootTag.getContent().add(rowTag);
        rowTag.getContent().add(HTMLElement.td("Total"));
        rowTag.getContent().add(HTMLElement.td(pData.getTotal()));
        rowTag.getContent().add(HTMLElement.td(pData.getCovered()));
        rowTag.getContent().add(HTMLElement.td(pData.getTotal() - pData.getCovered()));
        return tfootTag;
    }

    private HTMLElement createVariableInformation(final File cFile,
                                                  final String cName,
                                                  final Set<DefUsePair> pairs,
                                                  final ProgramVariable var,
                                                  final String color) {
        HTMLElement variableSpan = createVariableSpan(var, color);
        HTMLElement tooltipContent;
        tooltipContent = createVariableTooltipContent(cFile, cName, pairs, var);
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

    private HTMLElement createVariableTooltipContent(final File cFile,
                                                     final String cName,
                                                     final Set<DefUsePair> pairs,
                                                     final ProgramVariable var) {
        // Is var def / use / both?
        boolean isDefinition = isDefinition(var, pairs);
        boolean isUsage = isUsage(var, pairs);

        HTMLElement table = HTMLElement.table();
        table.getAttributes().add("class=\"white-gray rounded-corners\"");
        table.getContent().add(createTabButtons(var, isDefinition, isUsage));

        if (isDefinition) {
            Set<ProgramVariable> associateList = extractsAssociates(pairs, var);
            if (!associateList.isEmpty()) {
                table.getContent().add(createAssociatedVariablesSpan(var));
                table.getContent().add(createAssociatedVariablesTable(cFile, cName, var, associateList));
            }
            table.getContent().add(createUsagesSpan(var));
            table.getContent().add(createTabInfoTable(cFile, cName, var, pairs, true));
        }

        if (isUsage) {
            table.getContent().add(createDefinitionsSpan(var));
            table.getContent().add(createTabInfoTable(cFile, cName, var, pairs, false));
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

    private HTMLElement createTabInfoTable(final File cFile,
                                           final String cName,
                                           final ProgramVariable var,
                                           final Set<DefUsePair> pairs,
                                           final boolean isDefTab) {
        HTMLElement row = HTMLElement.tr();
        HTMLElement cell = HTMLElement.td();
        row.getContent().add(cell);
        HTMLElement div = HTMLElement.div();
        cell.getContent().add(div);
        HTMLElement table = HTMLElement.table();
        div.getContent().add(table);
        table.getAttributes().add("class=\"tooltipTable gray rounded-corners\"");
        table.getContent().add(createHeaderRow());

        if (isDefTab) {
            String defTab = String.format("class=\"%s%s%sDefTab margin10\"", var.getName(),
                    var.getLineNumber(), var.getInstructionIndex());
            div.getAttributes().add(defTab);
            Set<ProgramVariable> useList = pairs.stream()
                    .map(x -> CoverageDataStore.getInstance().getProgramVariableMap().get(x.getUseId()))
                    .collect(Collectors.toSet());
            for (ProgramVariable use : useList) {
                try {
                    table.getContent().add(createDataRow(cFile, cName, use.getLineNumber(), use.getIsCovered()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            String useTab = String.format("class=\"%s%s%sUseTab margin10\"", var.getName(),
                    var.getLineNumber(), var.getInstructionIndex());
            div.getAttributes().add(useTab);
            Set<ProgramVariable> defList = pairs.stream()
                    .map(x -> CoverageDataStore.getInstance().getProgramVariableMap().get(x.getDefId()))
                    .collect(Collectors.toSet());
            for (ProgramVariable def : defList) {
                try {
                    table.getContent().add(createDataRow(cFile, cName, def.getLineNumber(), def.getIsCovered()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return row;
    }

//    private Map<ProgramVariable, Boolean> getCoverageInformation(final Map<DefUsePair, Boolean> pVariableInfo,
//                                                                 final boolean isDefinitionTab) {
//        Map<ProgramVariable, Boolean> result = new TreeMap<>();
//        for (Map.Entry<DefUsePair, Boolean> entry : pVariableInfo.entrySet()) {
//            if (isDefinitionTab) {
//                ProgramVariable usage = entry.getKey().getUseId();
//                result.put(usage, entry.getValue());
//            } else {
//                ProgramVariable definition = entry.getKey().getDefId();
//                result.put(definition, entry.getValue());
//            }
//        }
//        return result;
//    }

    private HTMLElement createDataRow(final File cFile,
                                      final String cName,
                                      final int lNr,
                                      final Boolean isCovered) throws FileNotFoundException {
        HTMLElement row = HTMLElement.tr();
        HTMLElement lineCell = HTMLElement.td(lNr);
        lineCell.getAttributes().add("class=\"rightBorder centerText\"");
        row.getContent().add(lineCell);

        HTMLElement statementCell = HTMLElement.td();
        statementCell.getAttributes().add("class=\"nowrap\"");
        row.getContent().add(statementCell);

        String link = String.format("%s.java.html#L%s", cName, lNr);
        String statement = getLineFromFile(cFile, lNr);
        HTMLElement a = HTMLElement.a(link, statement);
        statementCell.getContent().add(a);
        if (isCovered) {
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

    private Set<ProgramVariable> extractsAssociates(final Set<DefUsePair> pairs,
                                                    final ProgramVariable var) {
        Set<ProgramVariable> result = new HashSet<>();
        for (DefUsePair element : pairs) {
            ProgramVariable def = CoverageDataStore.getInstance().getProgramVariableMap().get(element.getDefId());
            if (!def.equals(var)) {
                result.add(def);
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

    private HTMLElement createAssociatedVariablesTable(final File cFile,
                                                       final String cName,
                                                       final ProgramVariable var,
                                                       final Set<ProgramVariable> associateList) {
        String defTab = String.format("class=\"%s%s%sDefTab\"", var.getName(),
                var.getLineNumber(), var.getInstructionIndex());

        HTMLElement table = HTMLElement.table();
        table.getAttributes().add("class=\"margin10 gray rounded-corners\"");
        table.getContent().add(createHeaderRow());
        for (ProgramVariable v : associateList) {
            try {
                table.getContent().add(createDataRow(cFile, cName, v.getLineNumber(), v.getIsCovered()));
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

    private String getVariableBackgroundColor(boolean pIsVarCovered, Set<DefUsePair> pairSet) {
        if (!pIsVarCovered || pairSet.size() == 0 || pairSet.stream().noneMatch(DefUsePair::isCovered)) {
            return "red";
        } else if (pairSet.stream().allMatch(DefUsePair::isCovered)) {
            return "green";
        } else {
            return "yellow";
        }
    }

    private ProgramVariable findProgramVariable(final MethodData mData,
                                                final int lNr,
                                                final String name,
                                                final boolean isDef) {
        // TODO: Check possible vars
        List<ProgramVariable> possibleVariables = getPossibleVars(mData, lNr, name);
        if (!possibleVariables.isEmpty()) {
            if (isDef) {
                List<ProgramVariable> defs = possibleVariables.stream()
                        .filter(x -> x.getIsDefinition() && x.getName().equals(name))
                        .sorted(Comparator.comparing(ProgramVariable::getInstructionIndex))
                        .collect(Collectors.toList());
                if (defs.isEmpty()) {
                    return null;
                }

                // TODO: This seems weird
                return defs.get(0);
            }
            possibleVariables.sort(Comparator.comparing(ProgramVariable::getInstructionIndex));
            // TODO: This seems weird
            return possibleVariables.get(0);
        }
        return null;
    }

    private List<ProgramVariable> getPossibleVars(final MethodData mData,
                                                  final int lNr,
                                                  final String name) {
        List<ProgramVariable> result = new ArrayList<>();
        for (ProgramVariable element : mData.getProgramVariables().values()) {
            if (element.getLineNumber() == lNr
                    && element.getName().equals(name)
                    && !result.contains(element)) {
                result.add(element);
            }
        }

        return result;
    }

    private boolean isRedefined(MethodData mData, int pLineNumber, String pName) {
        for (DefUsePair pair : mData.getPairs().values()) {
            ProgramVariable def = CoverageDataStore.getInstance().getProgramVariableMap().get(pair.getDefId());
            // if another definition with the same name, but greater line number exists and
            // the current variable is not part of an active pair we know, that it must have been redefined
            if (def.getLineNumber() > pLineNumber && def.getName().equals(pName)
                    && !mData.isAnalyzedVariable(pName, pLineNumber)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDefinition(ProgramVariable pVariable, Set<DefUsePair> pDefUsePairs) {
        for (DefUsePair defUsePair : pDefUsePairs) {
            if (defUsePair.getDefId().equals(pVariable)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsage(ProgramVariable pVariable, Set<DefUsePair> pDefUsePairs) {
        for (DefUsePair defUsePair : pDefUsePairs) {
            if (defUsePair.getUseId().equals(pVariable)) {
                return true;
            }
        }
        return false;
    }

    // find all uses for one particular definition
    private Set<DefUsePair> getDefUsePairsCoveredForVar(MethodData mData, ProgramVariable pVariable) {
        Set<DefUsePair> result = new HashSet<>();
        for (DefUsePair element : mData.getPairs().values()) {
                if (element.getDefId().equals(pVariable) || element.getUseId().equals(pVariable)) {
                    result.add(element);
                }
        }
        return result;
    }
}
