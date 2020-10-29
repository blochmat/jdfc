package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.JDFCInstrument;
import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.ifg.data.DefUsePair;
import com.jdfc.core.analysis.ifg.data.Field;
import com.jdfc.core.analysis.ifg.data.InstanceVariable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;
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
            String relativePathWithType = jdfcPath.relativize(xml.toPath()).toString();
            String relativePath = relativePathWithType.split("\\.")[0];
            ExecutionDataNode<ExecutionData> classExecutionDataNode = CoverageDataStore.getInstance().findClassDataNode(relativePath);
            ClassExecutionData classExecutionData = (ClassExecutionData) classExecutionDataNode.getData();

            // ClassList works as worklist to keep track of loaded files
            CoverageDataStore.getInstance().getClassList().remove(relativePath);
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
                Node rootTag = doc.getFirstChild();
                examineFileRecursive(null, rootTag, classExecutionData);
                classExecutionData.calculateMethodCount();
                classExecutionData.calculateTotal();
                classExecutionData.computeCoverageForClass();
                classExecutionDataNode.setData(classExecutionData);
                classExecutionDataNode.aggregateDataToRoot();
            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
            }
        }

        List<String> classList = CoverageDataStore.getInstance().getClassList();
        JDFCInstrument JDFCInstrument = new JDFCInstrument();

        // If untested classes exist => compute def use pairs
        for (String relPath : classList) {
            String classFilePath = String.format("%s/%s%s", pClassesDir, relPath, classSuffix);
            File classFile = new File(classFilePath);
            try {
                byte[] classFileBuffer = Files.readAllBytes(classFile.toPath());
                ClassReader cr = new ClassReader(classFileBuffer);
                JDFCInstrument.instrument(cr);
                ExecutionDataNode<ExecutionData> classExecutionDataNode =
                        CoverageDataStore.getInstance().findClassDataNode(relPath);
                classExecutionDataNode.aggregateDataToRoot();
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

    private static void examineFileRecursive(String pParent, Node pXMLNode, ClassExecutionData classExecutionData) {
        NodeList nodeList = pXMLNode.getChildNodes();
        String methodName;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attr = node.getAttributes();
            if (attr != null) {
                switch (node.getNodeName()) {
                    case "fieldList":
                    case "instanceVariableList":
                    case "parameterMatching":
                    case "defUsePairs":
                    case "defUseCovered":
                        examineFileRecursive(node.getNodeName(), node, classExecutionData);
                        break;
                    case "match":
                        collectMatchData(node, classExecutionData);
                        break;
                    case "method":
                        Integer firstLine = Integer.valueOf(node.getAttributes().getNamedItem("firstLine").getNodeValue());
                        methodName = node.getAttributes().getNamedItem("name").getNodeValue();
                        classExecutionData.getMethodFirstLine().put(methodName, firstLine);
                        examineFileRecursive(node.getNodeName(), node, classExecutionData);
                        break;
                    case "defUsePair":
                        NamedNodeMap defUsePairAttr = node.getAttributes();
                        if(defUsePairAttr != null) {
                            methodName = node.getParentNode().getParentNode().getAttributes().getNamedItem("name").getNodeValue();
                            collectDefUsePairData(methodName, defUsePairAttr, classExecutionData);
                            break;
                        }
                    case "instanceVariable":
                        NamedNodeMap instanceVarAttr = node.getAttributes();
                        if (instanceVarAttr != null) {
                            collectInstanceVariableData(instanceVarAttr, classExecutionData);
                            break;
                        }
                    case "field":
                        NamedNodeMap fieldAttr = node.getAttributes();
                        if (fieldAttr != null) {
                            collectFieldData(fieldAttr, classExecutionData);
                            break;
                        }
                    case "programVariable":
                        NamedNodeMap programVarAttr = node.getAttributes();
                        if(programVarAttr != null) {
                            if (pParent.equals("defUseCovered")) {
                                methodName = node.getParentNode().getParentNode().getAttributes().getNamedItem("name").getNodeValue();
                                collectProgramVariableData(methodName, programVarAttr, classExecutionData);
                                break;
                            }
                        }
                    default:
                        throw new IllegalArgumentException("Invalid Tag in XML! " + node.getNodeName());
                }
            }
        }
    }

    private static void collectInstanceVariableData(NamedNodeMap pAttr, ClassExecutionData pClassExecutionData) {
        String owner = pAttr.getNamedItem("owner").getNodeValue();
        ProgramVariable holder = ProgramVariable.decode(pAttr.getNamedItem("holder").getNodeValue());
        int access = Integer.parseInt(pAttr.getNamedItem("access").getNodeValue());
        String name = pAttr.getNamedItem("name").getNodeValue();
        String descriptor = pAttr.getNamedItem("descriptor").getNodeValue();
        String signature = pAttr.getNamedItem("signature").getNodeValue();
        int instructionIndex = Integer.parseInt(pAttr.getNamedItem("instructionIndex").getNodeValue());
        int lineNumber = Integer.parseInt(pAttr.getNamedItem("lineNumber").getNodeValue());

        InstanceVariable newVar = InstanceVariable.create(owner, holder, access, name, descriptor, signature, instructionIndex, lineNumber);
        pClassExecutionData.getInstanceVariables().add(newVar);
    }

    private static void collectMatchData(Node pMatch, ClassExecutionData pClassExecutionData) {
        NodeList programVars = pMatch.getChildNodes();
        ProgramVariable fst = null;
        ProgramVariable snd;
        for(int i = 0; i < programVars.getLength(); i++) {
            NamedNodeMap programVarAttr = programVars.item(i).getAttributes();
            if(programVarAttr != null) {
                if(fst == null) {
                    fst = createProgramVariable(programVarAttr);
                } else {
                    snd = createProgramVariable(programVarAttr);
                    pClassExecutionData.getParameterMatching().add(new Pair<>(fst, snd));
                }
            }
        }
    }

    private static void collectFieldData(NamedNodeMap pAttr, ClassExecutionData pClassExecutionData) {
        String owner = pAttr.getNamedItem("owner").getNodeValue();
        int access = Integer.parseInt(pAttr.getNamedItem("access").getNodeValue());
        String name = pAttr.getNamedItem("name").getNodeValue();
        String descriptor = pAttr.getNamedItem("descriptor").getNodeValue();
        String signature = pAttr.getNamedItem("signature").getNodeValue();
        Field newField = Field.create(owner, access, name, descriptor, signature);
        pClassExecutionData.getFields().add(newField);
    }

    private static void collectProgramVariableData(String pMethodName, NamedNodeMap pAttr, ClassExecutionData pClassExecutionData) {
        ProgramVariable programVariable = createProgramVariable(pAttr);
        if(pClassExecutionData.getDefUseCovered().get(pMethodName) == null) {
            Set<ProgramVariable> defUseCovered = new HashSet<>();
            defUseCovered.add(programVariable);
            pClassExecutionData.getDefUseCovered().put(pMethodName, defUseCovered);
        } else {
            pClassExecutionData.getDefUseCovered().get(pMethodName).add(programVariable);
        }
    }

    private static void collectDefUsePairData(String pMethodName, NamedNodeMap pAttr, ClassExecutionData pClassExecutionData) {
        DefUsePair defUsePair = createDefUsePair(pAttr);
        if(pClassExecutionData.getDefUsePairs().get(pMethodName) == null) {
            List<DefUsePair> defUsePairs = new ArrayList<>();
            defUsePairs.add(defUsePair);
            pClassExecutionData.getDefUsePairs().put(pMethodName, defUsePairs);
        } else {
            pClassExecutionData.getDefUsePairs().get(pMethodName).add(defUsePair);
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

    private static DefUsePair createDefUsePair(NamedNodeMap pAttr) {
        String dOwner = pAttr.getNamedItem("dOwner").getNodeValue();
        String dName = pAttr.getNamedItem("dName").getNodeValue();
        String dType = pAttr.getNamedItem("dType").getNodeValue();
        int definitionIndex = Integer.parseInt(pAttr.getNamedItem("dIndex").getNodeValue());
        int definitionLineNumber = Integer.parseInt(pAttr.getNamedItem("dLineNumber").getNodeValue());
        ProgramVariable definition = ProgramVariable.create(dOwner, dName, dType, definitionIndex, definitionLineNumber);

        String uOwner = pAttr.getNamedItem("uOwner").getNodeValue();
        String uName = pAttr.getNamedItem("uName").getNodeValue();
        String uType = pAttr.getNamedItem("uType").getNodeValue();
        int uIndex = Integer.parseInt(pAttr.getNamedItem("uIndex").getNodeValue());
        int uLineNumber = Integer.parseInt(pAttr.getNamedItem("uLineNumber").getNodeValue());
        ProgramVariable usage = ProgramVariable.create(uOwner, uName, uType, uIndex, uLineNumber);
        return new DefUsePair(definition, usage);
    }
}
