package data;

import instr.JDFCInstrument;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import utils.JDFCUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class CoverageDataExport {

    private static final Logger logger = LoggerFactory.getLogger(CoverageDataExport.class);

    public static void init() {
        logger.debug("CoverageDataExport initialized.");
    }

    public static void dumpCoverageDataToFile() throws ParserConfigurationException, TransformerException {
        // Create JDFC directory
        String outPath = String.format("%s%starget%sjdfc", System.getProperty("user.dir"), File.separator, File.separator);
        File JDFCDir = new File(outPath);
        if (!JDFCDir.exists()) {
            JDFCDir.mkdirs();
        }

        analyseUntestedClasses();
        CoverageDataStore.getInstance().getRoot().computeCoverage();

        Set<ExecutionData> exDataSet = treeToSetRecursive(CoverageDataStore.getInstance().getRoot());

        // Actual output
        String classXMLPath = String.format("%s%s%s.xml", outPath, File.separator, "jdfc-coverage");

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Optional<ExecutionData> rootDataOptional = exDataSet.stream().filter(data -> Objects.equals(data.getFqn(), "")).findFirst();
        if(!rootDataOptional.isPresent()) {
            throw new RuntimeException("Root data not present.");
        }

        // create coverage element with root data
        ExecutionData rootData = rootDataOptional.get();
        exDataSet.remove(rootData);
        Element coverage = doc.createElement("coverage");
        coverage.setAttribute("pair-rate", String.valueOf(rootData.getRate()));
        coverage.setAttribute("pairs-covered", String.valueOf(rootData.getCovered()));
        coverage.setAttribute("pairs-valid", String.valueOf(rootData.getTotal()));
        coverage.setAttribute("version", "1.0-SNAPSHOT");
        coverage.setAttribute("timestamp", String.valueOf(System.currentTimeMillis()));
        doc.appendChild(coverage);

        Set<ExecutionData> pkgDataSet = exDataSet.stream()
                .filter(data -> !(data instanceof ClassExecutionData))
                .collect(Collectors.toSet());
        Set<ClassExecutionData> cDataSet = exDataSet.stream()
                .filter(data -> data instanceof ClassExecutionData)
                .map(data -> (ClassExecutionData) data)
                .collect(Collectors.toSet());

        // create sources
        Element sources = doc.createElement("sources");
        coverage.appendChild(sources);

        // fill sources
        String projectDirString = CoverageDataStore.getInstance().getProjectDirStr();
        for(String src : CoverageDataStore.getInstance().getSrcDirStrList()) {
            Element source = doc.createElement("source");
            // substring(1) to remove first /
            String relPath = JDFCUtils.getStringDiff(projectDirString, src).substring(1);
            source.setTextContent(relPath);
            sources.appendChild(source);
        }

        // create packages
        Element packages = doc.createElement("packages");
        coverage.appendChild(packages);

        for(ExecutionData pkgData : pkgDataSet) {
            // create package
            Element pkg = doc.createElement("package");
            pkg.setAttribute("name", pkgData.getFqn());
            pkg.setAttribute("pair-rate", String.valueOf(pkgData.getRate()));
            packages.appendChild(pkg);

            // add classes of package
            Element classes = doc.createElement("classes");
            pkg.appendChild(classes);

            for(ClassExecutionData cData : cDataSet) {
                if (Objects.equals(pkgData.getFqn(), cData.getParentFqn())) {
                    Element clazz = doc.createElement("class");
                    clazz.setAttribute("name", cData.getFqn());
                    clazz.setAttribute("filename", cData.getRelativePath());
                    classes.appendChild(clazz);

                    Element methods = doc.createElement("methods");
                    clazz.appendChild(methods);

                    for(MethodData mData : cData.getMethods().values()) {
                        Element method = doc.createElement("method");
                        method.setAttribute("name", mData.getName());
                        method.setAttribute("signature", mData.getSignature());
                        method.setAttribute("pair-rate", String.valueOf(mData.getRate()));
                        methods.appendChild(method);

                        Element pairs = doc.createElement("pairs");
                        method.appendChild(pairs);

                        for(DefUsePair pData : mData.getPairs()) {

                            Element pair = doc.createElement("pair");
                            pair.setAttribute("type", pData.getType());
                            pair.setAttribute("covered", String.valueOf(pData.isCovered()));
                            pairs.appendChild(pair);

                            ProgramVariable d = pData.getDefinition();
                            Element def = doc.createElement("def");
                            def.setAttribute("name", d.getName());
                            def.setAttribute("line", String.valueOf(d.getLineNumber()));
                            def.setAttribute("idx", String.valueOf(d.getInstructionIndex()));
                            pair.appendChild(def);

                            ProgramVariable u = pData.getUsage();
                            Element use = doc.createElement("use");
                            use.setAttribute("name", u.getName());
                            use.setAttribute("line", String.valueOf(u.getLineNumber()));
                            use.setAttribute("idx", String.valueOf(u.getInstructionIndex()));
                            pair.appendChild(use);
                        }
                    }
                }
            }
        }

        // The end.
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        File file = new File(classXMLPath);
        file.getParentFile().mkdirs();
        try {
            OutputStream out = new FileOutputStream(file);
            StreamResult streamResult = new StreamResult(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
            transformer.transform(new DOMSource(doc), streamResult);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Set<ExecutionData> treeToSetRecursive(ExecutionDataNode<ExecutionData> node) {
        Set<ExecutionData> result = new HashSet<>();
        if (node.isRoot() || node.isLeaf() || node.containsLeafs()) {
            ExecutionData data = node.getData();
            result.add(data);
        }

        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> child : node.getChildren().entrySet()) {
            result.addAll(treeToSetRecursive(child.getValue()));
        }
        return result;
    }

    private static void analyseUntestedClasses() {
        List<String> classList = CoverageDataStore.getInstance().getUntestedClassList();
        JDFCInstrument JDFCInstrument = new JDFCInstrument();

        for (String relPath : classList) {
            // pClassesDir = target/classes
            String classFilePath = String.format("%s/%s%s", CoverageDataStore.getInstance().getClassesBuildDirStr(), relPath, ".class");
            File classFile = new File(classFilePath);
            try {
                byte[] classFileBuffer = Files.readAllBytes(classFile.toPath());
                ClassReader cr = new ClassReader(classFileBuffer);
                JDFCInstrument.instrument(cr);
                ExecutionDataNode<ExecutionData> classExecutionDataNode =
                        CoverageDataStore.getInstance().findClassDataNode(relPath);
                ClassExecutionData classExecutionData = (ClassExecutionData) classExecutionDataNode.getData();
                classExecutionData.computeCoverageForClass();
                classExecutionDataNode.aggregateDataToRootRecursive();
                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException | TransformerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Creating xml file representing coverage data of a single class
     * @param pClassExecutionData Class data
     * @throws ParserConfigurationException Occurs in case of failing to build the document
     * @throws TransformerException Occurs in case of failing to transform the built file into xml
     */
    public static void dumpClassExecutionDataToFile(final ClassExecutionData pClassExecutionData) throws ParserConfigurationException, TransformerException {
        String outPath = String.format("%s%starget%sjdfc", System.getProperty("user.dir"), File.separator, File.separator);
        File JDFCDir = new File(outPath);
        if (!JDFCDir.exists()) {
            JDFCDir.mkdirs();
        }

        String classXMLPath = String.format("%s%s%s.xml", outPath, File.separator, pClassExecutionData.getRelativePath());

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element classTag = doc.createElement("class");
        doc.appendChild(classTag);

        Set<InterProceduralMatch> interProceduralMatches = pClassExecutionData.getInterProceduralMatches();
        classTag.appendChild(createInterProceduralMatchList(doc, interProceduralMatches));

        Map<String, Integer> methodFirstLine = pClassExecutionData.getMethodFirstLine();
        Map<String, Integer> methodLastLine = pClassExecutionData.getMethodLastLine();
        TreeMap<String, List<DefUsePair>> defUsePairs = pClassExecutionData.getDefUsePairs();
        Map<String, Set<ProgramVariable>> variablesCovered = pClassExecutionData.getVariablesCovered();
        for (Map.Entry<String, List<DefUsePair>> methodEntry : defUsePairs.entrySet()) {
            if (methodEntry.getValue().size() != 0) {
                String methodName = methodEntry.getKey();
                classTag.appendChild(createMethod(doc, methodName,
                        methodFirstLine.get(methodName), methodLastLine.get(methodName),
                        methodEntry.getValue(), variablesCovered.get(methodName)));
            }
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        File file = new File(classXMLPath);
        file.getParentFile().mkdirs();
        try {
            OutputStream out = new FileOutputStream(file);
            StreamResult streamResult = new StreamResult(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
            transformer.transform(new DOMSource(doc), streamResult);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private static Element createInterProceduralMatchList(Document pDoc, Set<InterProceduralMatch> pParameterMatches) {
        Element interProceduralMatches = pDoc.createElement("interProceduralMatchList");
        for(InterProceduralMatch element : pParameterMatches) {
            Element match = pDoc.createElement("match");
            match.setAttribute("methodName", element.getMethodName());
            match.setAttribute("callSiteMethodName", element.getCallSiteMethodName());
            interProceduralMatches.appendChild(match);
            Element definition = createProgramVariable(pDoc, element.getDefinition());
            match.appendChild(definition);
            Element callSiteDefinition = createProgramVariable(pDoc, element.getCallSiteDefinition());
            match.appendChild(callSiteDefinition);
        }
        return interProceduralMatches;
    }

    private static Element createMethod(Document pDoc,
                                        String pMethodName,
                                        Integer pMethodFirstLine,
                                        Integer pMethodLastLine,
                                        List<DefUsePair> pDefUsePairs,
                                        Set<ProgramVariable> pDefUseCovered) {
        Element methodTag = pDoc.createElement("method");
        methodTag.setAttribute("name", pMethodName);
        methodTag.setAttribute("firstLine", String.valueOf(pMethodFirstLine));
        methodTag.setAttribute("lastLine", String.valueOf(pMethodLastLine));
        methodTag.appendChild(createDefUsePairs(pDoc, pDefUsePairs));
        if (pDefUseCovered != null) {
            methodTag.appendChild(createVariablesCovered(pDoc, pDefUseCovered));
        }
        return methodTag;
    }

    private static Element createDefUsePairs(Document pDoc, List<DefUsePair> pDefUsePairList) {
        Element defUsePairList = pDoc.createElement("defUsePairList");
        for (DefUsePair element : pDefUsePairList) {
            Element defUsePair = pDoc.createElement("defUsePair");
            defUsePairList.appendChild(defUsePair);
            defUsePair.setAttribute("dOwner", element.getDefinition().getOwner());
            defUsePair.setAttribute("dName", element.getDefinition().getName());
            defUsePair.setAttribute("dType", element.getDefinition().getDescriptor());
            defUsePair.setAttribute("dIndex", Integer.toString(element.getDefinition().getInstructionIndex()));
            defUsePair.setAttribute("dLineNumber", Integer.toString(element.getDefinition().getLineNumber()));
            defUsePair.setAttribute("dIsDefinition", Boolean.toString(element.getDefinition().isDefinition()));

            defUsePair.setAttribute("uOwner", element.getUsage().getOwner());
            defUsePair.setAttribute("uName", element.getUsage().getName());
            defUsePair.setAttribute("uType", element.getUsage().getDescriptor());
            defUsePair.setAttribute("uIndex", Integer.toString(element.getUsage().getInstructionIndex()));
            defUsePair.setAttribute("uLineNumber", Integer.toString(element.getUsage().getLineNumber()));
            defUsePair.setAttribute("uIsDefinition", Boolean.toString(element.getUsage().isDefinition()));

        }
        return defUsePairList;
    }

    private static Element createVariablesCovered(Document pDoc, Set<ProgramVariable> pProgramVarsCovered) {
        Element variablesCoveredList = pDoc.createElement("variablesCoveredList");
        for (ProgramVariable element : pProgramVarsCovered) {
            Element programVariableTag = createProgramVariable(pDoc, element);
            variablesCoveredList.appendChild(programVariableTag);
        }
        return variablesCoveredList;
    }

    private static Element createProgramVariable(Document pDoc, ProgramVariable pProgramVariable) {
        Element programVariable = pDoc.createElement("programVariable");
        programVariable.setAttribute("owner", pProgramVariable.getOwner());
        programVariable.setAttribute("name", pProgramVariable.getName());
        programVariable.setAttribute("descriptor", pProgramVariable.getDescriptor());
        programVariable.setAttribute("instructionIndex",
                Integer.toString(pProgramVariable.getInstructionIndex()));
        programVariable.setAttribute("lineNumber",
                Integer.toString(pProgramVariable.getLineNumber()));
        programVariable.setAttribute("isDefinition",
                Boolean.toString(pProgramVariable.isDefinition()));
        return programVariable;
    }
}
