package data.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import data.*;
import data.singleton.CoverageDataStore;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CoverageDataExport {

    // DO NOT USE. LOG TO FILE. See logThis method at the bottom.
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

        CoverageDataExport.analyseUntestedClasses();
        CoverageDataStore.getInstance().getRoot().computeClassCoverage();
        CoverageDataStore.getInstance().getRoot().aggregateDataToRootRecursive();

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

                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            objectMapper.writeValue(new File(String.format("/tmp/%s.json", mData.getName())), mData);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        Element method = doc.createElement("method");
                        method.setAttribute("name", mData.getName());
                        method.setAttribute("signature", mData.getDesc());
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
        logger.debug("treeToSetRecursive");
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
        logger.debug("analyseUntestedClasses");
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
                classExecutionData.computeCoverage();
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
        logger.debug("dumpClassExecutionDataToFile");
        String outPath = String.format("%s%starget%sjdfc", System.getProperty("user.dir"), File.separator, File.separator);
        File JDFCDir = new File(outPath);
        if (!JDFCDir.exists()) {
            JDFCDir.mkdirs();
        }

        String filePath = String.format("%s%s%s.json", outPath, File.separator, pClassExecutionData.getRelativePath());
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(file, pClassExecutionData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logThis(String str) {
        String outPath = String.format("%s%starget%sjdfc", System.getProperty("user.dir"), File.separator, File.separator);
        String filePath = String.format("%s/custom.log", outPath);

        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(getFormattedTimestamp() + " - " + str);
            writer.write("\n");
        } catch (IOException ioException) {
            ioException.printStackTrace();  // print to console as a last resort
        }
    }

    public static String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }
}
