package report;

import com.fasterxml.jackson.databind.ObjectMapper;
import data.*;
import data.singleton.CoverageDataStore;
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

public class XMLReportGenerator {

    private final String outputDirAbs;

    public XMLReportGenerator(String outputDirAbs) {
        this.outputDirAbs = outputDirAbs;
    }

    public void create() {
        // Create JDFC directory
        File JDFCDir = new File(outputDirAbs);
        if (!JDFCDir.exists()) {
            JDFCDir.mkdirs();
        }

        // Actual output
        String classXMLPath = String.join(File.separator, outputDirAbs, "/coverage.xml");

        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            // Project
            Element coverage = doc.createElement("coverage");
            coverage.setAttribute("pair-rate", String.valueOf(CoverageDataStore.getInstance().getRate()));
            coverage.setAttribute("pairs-covered", String.valueOf(CoverageDataStore.getInstance().getCovered()));
            coverage.setAttribute("pairs-valid", String.valueOf(CoverageDataStore.getInstance().getTotal()));
            coverage.setAttribute("version", "1.0-SNAPSHOT");
            coverage.setAttribute("timestamp", String.valueOf(System.currentTimeMillis()));
            doc.appendChild(coverage);

            // Sources
            Element sources = doc.createElement("sources");
            coverage.appendChild(sources);
            File projectDirString = CoverageDataStore.getInstance().getWorkDir();
            Element source = doc.createElement("source");
            String relPath = JDFCUtils.getStringDiff(String.valueOf(projectDirString), CoverageDataStore.getInstance().getSourceDirAbs()).substring(1);
            source.setTextContent(relPath);
            sources.appendChild(source);

            // Packages
            Element packages = doc.createElement("packages");
            coverage.appendChild(packages);

            for(PackageData pkgData : CoverageDataStore.getInstance().getPackageDataMap().values()) {
                // create package
                Element pkg = doc.createElement("package");
                pkg.setAttribute("name", pkgData.getFqn());
                pkg.setAttribute("pair-rate", String.valueOf(pkgData.getRate()));
                packages.appendChild(pkg);

                // add classes of package
                Element classes = doc.createElement("classes");
                pkg.appendChild(classes);

                for (ClassData cData : pkgData.getClassDataFromStore().values()) {
//                    Set<ProgramVariable> fieldDefinitions = cData.getFieldDefinitions().values().stream()
//                            .flatMap(inner -> inner.values().stream())
//                            .collect(Collectors.toSet());
//                    JDFCUtils.logThis(cData.getRelativePath() + "\n" + JDFCUtils.prettyPrintSet(fieldDefinitions), "fieldDefinitions");
                    Element clazz = doc.createElement("class");
                    clazz.setAttribute("name", cData.getFqn());
                    clazz.setAttribute("filename", cData.getRelativePath());
                    classes.appendChild(clazz);

                    Element methods = doc.createElement("methods");
                    clazz.appendChild(methods);

                    for (MethodData mData : cData.getMethodDataFromStore().values()) {
//                        JDFCUtils.logThis(cData.getRelativePath() + " " + mData.buildInternalMethodName() + "\n" + JDFCUtils.prettyPrintMap(mData.getAllocatedObjects()), "allocatedObjects");
//                        JDFCUtils.logThis(cData.getRelativePath() + " " + mData.buildInternalMethodName() + "\n" + JDFCUtils.prettyPrintMap(mData.getModifiedObjects()), "modifiedObjects");
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

                        for (DefUsePair pData : mData.getDUPairsFromStore().values()) {
                            ProgramVariable d = CoverageDataStore.getInstance().getProgramVariableMap().get(pData.getDefId());
                            ProgramVariable u = CoverageDataStore.getInstance().getProgramVariableMap().get(pData.getUseId());

                            Element pair = doc.createElement("pair");
                            pair.setAttribute("uuid", String.valueOf(pData.getId()));
                            pair.setAttribute("type", d.getDescriptor());
                            pair.setAttribute("covered", String.valueOf(pData.isCovered()));
                            pairs.appendChild(pair);

                            Element def = doc.createElement("def");
                            def.setAttribute("name", d.getName());
                            def.setAttribute("line", String.valueOf(d.getLineNumber()));
                            def.setAttribute("idx", String.valueOf(d.getInstructionIndex()));
                            pair.appendChild(def);

                            Element use = doc.createElement("use");
                            use.setAttribute("name", u.getName());
                            use.setAttribute("line", String.valueOf(u.getLineNumber()));
                            use.setAttribute("idx", String.valueOf(u.getInstructionIndex()));
                            pair.appendChild(use);
                        }
                    }

                    Element pairs = doc.createElement("pairs");
                    clazz.appendChild(pairs);

                    int idCounter = 0;
                    for (DefUsePair pairData : CoverageDataStore.getInstance().getDefUsePairMap().values()) {
                        if(cData.getRelativePath().equals(pairData.getClassName())) {
                            Element pair = doc.createElement("pair");
                            pair.setAttribute("id", String.valueOf(idCounter));
                            pair.setAttribute("covered", String.valueOf(pairData.isCovered()));
                            pair.setAttribute("uuid", String.valueOf(pairData.getId()));
                            pairs.appendChild(pair);
                            idCounter++;
                        }
                    }
                }
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            File file = new File(classXMLPath);
            file.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(file);
            StreamResult streamResult = new StreamResult(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
            transformer.transform(new DOMSource(doc), streamResult);
        } catch (ParserConfigurationException | FileNotFoundException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
