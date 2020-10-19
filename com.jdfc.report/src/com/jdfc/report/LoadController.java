package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.JDFCInstrument;
import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.ifg.DefUsePair;
import com.jdfc.core.analysis.ifg.InstanceVariable;
import com.jdfc.core.analysis.ifg.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;
import org.objectweb.asm.ClassReader;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LoadController {

    public static void loadExecutionData(String pClassesDir, String pJDFCDir) {
        File classes = new File(pClassesDir);
        Path classesPath = classes.toPath();
        String classSuffix = ".class";


        File jdfc = new File(pJDFCDir);
        Path jdfcPath = jdfc.toPath();
        String xmlSuffix = ".xml";

        // Loading data node structure from target/classes
        CoverageDataStore.getInstance()
                .addNodesFromDirRecursive(classes, CoverageDataStore.getInstance().getRoot(), classesPath, classSuffix);

        // Load xml files from target/jdfc
        List<File> xmlFiles = loadFilesFromDirRecursive(jdfc, xmlSuffix);

        for (File xml : xmlFiles) {
            String relativePath = jdfcPath.relativize(xml.toPath()).toString();
            String relativePathWithoutType = relativePath.split("\\.")[0];
            ExecutionDataNode<ExecutionData> classExecutionDataNode = CoverageDataStore.getInstance().findClassDataNode(relativePathWithoutType);
            ClassExecutionData classExecutionData = (ClassExecutionData) classExecutionDataNode.getData();

            // ClassList works as worklist to keep track of loaded files
            CoverageDataStore.getInstance().getClassList().remove(relativePathWithoutType);
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
                Node rootTag = doc.getFirstChild();
                examineFileRecursive(rootTag, classExecutionData);
                classExecutionDataNode.setData(classExecutionData);
                classExecutionDataNode.aggregateDataToRoot();
            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
            }
        }

        List<String> classList = CoverageDataStore.getInstance().getClassList();
        JDFCInstrument JDFCInstrument = new JDFCInstrument();
        List<File> classFiles = new ArrayList<>();

        // If untested classes exist => compute def use pairs
        for (String relPath : classList) {
            String classFilePath = String.format("%s/%s%s", pClassesDir, relPath, classSuffix);
            File classFile = new File(classFilePath);
            classFiles.add(classFile);
        }

        for (File classFile : classFiles) {
            try {
                byte[] classFileBuffer = Files.readAllBytes(classFile.toPath());
                ClassReader cr = new ClassReader(classFileBuffer);
                JDFCInstrument.instrument(cr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<File> loadFilesFromDirRecursive(File file, String suffix) {
        List<File> returnList = new ArrayList<>();
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                returnList.addAll(loadFilesFromDirRecursive(child, suffix));
            }
        } else {
            if (file.getName().endsWith(suffix)) {
                returnList.add(file);
            }
        }
        return returnList;
    }

    private static void examineFileRecursive(Node pXMLNode, ClassExecutionData classExecutionData) {
        NodeList nodeList = pXMLNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attr = node.getAttributes();
            if (attr != null) {
                switch (node.getNodeName()) {
                    case "instanceVariables":
                        NodeList instanceVariables = node.getChildNodes();
                        getInstanceVariableData(instanceVariables, classExecutionData);
                        break;
                    case "parameterMatching":
                        NodeList matching = node.getChildNodes();
                        getParameterMatching(matching, classExecutionData);
                        break;
                    case "method":
                        examineFileRecursive(node, classExecutionData);
                        break;
                    default:
                        // DefUsePair or DefUseCovered list
                        String methodName = node.getParentNode().getAttributes().getNamedItem("name").getNodeValue();
                        Integer firstLine = Integer.valueOf(node.getParentNode().getAttributes().getNamedItem("firstLine").getNodeValue());
                        classExecutionData.getMethodFirstLine().put(methodName, firstLine);
                        NodeList defUsePairs = node.getChildNodes();
                        String listName = node.getNodeName();
                        collectCoverageData(methodName, defUsePairs, listName, classExecutionData);
                        break;
                }
            }
        }
    }

    private static void getParameterMatching(NodeList pMatching, ClassExecutionData pClassExecutionData) {
        for (int i = 0; i < pMatching.getLength(); i++) {
            ProgramVariable fst = null;
            ProgramVariable snd;
            Node match = pMatching.item(i);
            NamedNodeMap matchAttr = match.getAttributes();
            if (matchAttr != null) {
                NodeList programVariables = match.getChildNodes();
                for (int j = 0; j < programVariables.getLength(); j++) {
                    Node programVariable = programVariables.item(j);
                    NamedNodeMap attr = programVariable.getAttributes();
                    if (attr != null) {
                        if (fst == null) {
                            fst = createProgramVariable(attr);
                        } else {
                            snd = createProgramVariable(attr);
                            pClassExecutionData.getParameterMatching().add(new Pair<>(fst, snd));
                        }
                    }
                }
            }
        }
    }

    private static void getInstanceVariableData(NodeList pInstanceVariables, ClassExecutionData classExecutionData) {
        for (int i = 0; i < pInstanceVariables.getLength(); i++) {
            Node instanceVariable = pInstanceVariables.item(i);
            NamedNodeMap attr = instanceVariable.getAttributes();
            if (attr != null) {
                String owner = attr.getNamedItem("owner").getNodeValue();
                int access = Integer.parseInt(attr.getNamedItem("access").getNodeValue());
                String name = attr.getNamedItem("name").getNodeValue();
                String descriptor = attr.getNamedItem("descriptor").getNodeValue();
                String signature = attr.getNamedItem("signature").getNodeValue();
                int lineNumber = Integer.parseInt(attr.getNamedItem("lineNumber").getNodeValue());

                InstanceVariable newVar = InstanceVariable.create(owner, access, name, descriptor, signature, lineNumber);

                NodeList outScopeList = instanceVariable.getChildNodes();
                for (int j = 0; j < outScopeList.getLength(); j++) {
                    Node outScope = outScopeList.item(j);
                    NamedNodeMap outScopeAttr = outScope.getAttributes();
                    if (outScopeAttr != null) {
                        Integer methodFirstLine = Integer.valueOf(outScopeAttr.getNamedItem("fstLine").getNodeValue());
                        Integer methodLastLine = Integer.valueOf(outScopeAttr.getNamedItem("sndLine").getNodeValue());
                        newVar.getOutOfScope().put(methodFirstLine, methodLastLine);
                    }
                }
                classExecutionData.getInstanceVariables().add(newVar);
            }
        }
    }

    private static void collectCoverageData(String methodName, NodeList nodeList, String list, ClassExecutionData classExecutionData) {
        switch (list) {
            case "defUsePairs":
                classExecutionData.getDefUsePairs().put(methodName, new ArrayList<>());
                for (int j = 0; j < nodeList.getLength(); j++) {
                    Node pair = nodeList.item(j);
                    NamedNodeMap pairAttr = pair.getAttributes();
                    if (pairAttr != null) {
                        String dOwner = pairAttr.getNamedItem("dOwner").getNodeValue();
                        String dName = pairAttr.getNamedItem("dName").getNodeValue();
                        String dType = pairAttr.getNamedItem("dType").getNodeValue();
                        int definitionIndex = Integer.parseInt(pairAttr.getNamedItem("dIndex").getNodeValue());
                        int definitionLineNumber = Integer.parseInt(pairAttr.getNamedItem("dLineNumber").getNodeValue());
                        ProgramVariable definition = ProgramVariable.create(dOwner, dName, dType, definitionIndex, definitionLineNumber);

                        String uOwner = pairAttr.getNamedItem("uOwner").getNodeValue();
                        String uName = pairAttr.getNamedItem("uName").getNodeValue();
                        String uType = pairAttr.getNamedItem("uType").getNodeValue();
                        int uIndex = Integer.parseInt(pairAttr.getNamedItem("uIndex").getNodeValue());
                        int uLineNumber = Integer.parseInt(pairAttr.getNamedItem("uLineNumber").getNodeValue());
                        ProgramVariable usage = ProgramVariable.create(uOwner, uName, uType, uIndex, uLineNumber);

                        DefUsePair newPair = new DefUsePair(definition, usage);
                        classExecutionData.getDefUsePairs().get(methodName).add(newPair);
                    }
                }
                break;
            case "defUseCovered":
                classExecutionData.getDefUseCovered().put(methodName, new HashSet<>());
                for (int j = 0; j < nodeList.getLength(); j++) {
                    Node pair = nodeList.item(j);
                    NamedNodeMap pairAttr = pair.getAttributes();
                    if (pairAttr != null) {
                        ProgramVariable programVariable = createProgramVariable(pairAttr);
                        classExecutionData.getDefUseCovered().get(methodName).add(programVariable);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid Tag in XML!");
        }
    }

    private static ProgramVariable createProgramVariable(NamedNodeMap pAttr) {
        String owner = pAttr.getNamedItem("owner").getNodeValue();
        String name = pAttr.getNamedItem("name").getNodeValue();
        String type = pAttr.getNamedItem("type").getNodeValue();
        int instructionIndex = Integer.parseInt(pAttr.getNamedItem("instructionIndex").getNodeValue());
        int lineNumber = Integer.parseInt(pAttr.getNamedItem("lineNumber").getNodeValue());
        return ProgramVariable.create(owner, name, type, instructionIndex, lineNumber);
    }
}
