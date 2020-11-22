package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.JDFCInstrument;
import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.data.InterProceduralMatch;
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

public class CoverageDataImport {

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
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
                Node rootTag = doc.getFirstChild();
                examineFileRecursive(rootTag, classExecutionData);

                // If at least one variable is covered => tests exist for class
                if(!classExecutionData.getVariablesCovered().isEmpty()) {
                    CoverageDataStore.getInstance().getClassList().remove(relativePath);
                }
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
                ClassExecutionData classExecutionData = (ClassExecutionData) classExecutionDataNode.getData();
                classExecutionData.computeCoverageForClass();
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

    private static void examineFileRecursive(Node pXMLNode, ClassExecutionData classExecutionData) {
        NodeList nodeList = pXMLNode.getChildNodes();
        String methodName;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attr = node.getAttributes();
            if (attr != null) {
                switch (node.getNodeName()) {
                    case "fieldList":
                    case "instanceVariableList":
                    case "interProceduralMatchList":
                    case "defUsePairList":
                    case "variablesCoveredList":
                        examineFileRecursive(node, classExecutionData);
                        break;
                    case "match":
                        collectMatchData(node, classExecutionData);
                        break;
                    case "method":
                        methodName = node.getAttributes().getNamedItem("name").getNodeValue();
                        Integer firstLine = Integer.valueOf(node.getAttributes().getNamedItem("firstLine").getNodeValue());
                        Integer lastLine = Integer.valueOf(node.getAttributes().getNamedItem("lastLine").getNodeValue());
                        classExecutionData.getMethodFirstLine().put(methodName, firstLine);
                        classExecutionData.getMethodLastLine().put(methodName, lastLine);
                        examineFileRecursive(node, classExecutionData);
                        break;
                    case "defUsePair":
                        NamedNodeMap defUsePairAttr = node.getAttributes();
                        if (defUsePairAttr != null) {
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
//                    case "field":
//                        NamedNodeMap fieldAttr = node.getAttributes();
//                        if (fieldAttr != null) {
//                            collectFieldData(fieldAttr, classExecutionData);
//                            break;
//                        }
                    case "programVariable":
                        NamedNodeMap programVarAttr = node.getAttributes();
                        if (programVarAttr != null) {
                            methodName = node.getParentNode().getParentNode().getAttributes().getNamedItem("name").getNodeValue();
                            collectProgramVariableData(methodName, programVarAttr, classExecutionData);
                            break;

                        }
                    default:
                        throw new IllegalArgumentException("Invalid Tag in XML! " + node.getNodeName());
                }
            }
        }
    }

    private static void collectInstanceVariableData(NamedNodeMap pAttr, ClassExecutionData pClassExecutionData) {
        String owner = getValueOrNull(pAttr.getNamedItem("owner").getNodeValue());
        ProgramVariable holder = ProgramVariable.decode(pAttr.getNamedItem("holder").getNodeValue());
        String method = getValueOrNull(pAttr.getNamedItem("method").getNodeValue());
        String name = getValueOrNull(pAttr.getNamedItem("name").getNodeValue());
        String descriptor = getValueOrNull(pAttr.getNamedItem("descriptor").getNodeValue());
        int instructionIndex = Integer.parseInt(pAttr.getNamedItem("instructionIndex").getNodeValue());
        int lineNumber = Integer.parseInt(pAttr.getNamedItem("lineNumber").getNodeValue());
        boolean isDefinition = Boolean.parseBoolean(pAttr.getNamedItem("isDefinition").getNodeValue());
        InstanceVariable newVar = InstanceVariable.create(owner, holder, method, name, descriptor, instructionIndex, lineNumber, isDefinition);
        pClassExecutionData.getInstanceVariables().add(newVar);
    }

    private static void collectMatchData(Node pMatch, ClassExecutionData pClassExecutionData) {
        String methodName = pMatch.getAttributes().getNamedItem("methodName").getNodeValue();
        String callSiteMethodName = pMatch.getAttributes().getNamedItem("callSiteMethodName").getNodeValue();
        NodeList programVars = pMatch.getChildNodes();
        ProgramVariable definition = null;
        ProgramVariable callSiteDefinition;
        for (int i = 0; i < programVars.getLength(); i++) {
            NamedNodeMap programVarAttr = programVars.item(i).getAttributes();
            if (programVarAttr != null) {
                if (definition == null) {
                    definition = createProgramVariable(programVarAttr);
                } else {
                    callSiteDefinition = createProgramVariable(programVarAttr);
                    InterProceduralMatch newMatch = InterProceduralMatch.create(definition, callSiteDefinition, methodName, callSiteMethodName);
                    pClassExecutionData.getInterProceduralMatches().add(newMatch);
                }
            }
        }
    }

//    private static void collectFieldData(NamedNodeMap pAttr, ClassExecutionData pClassExecutionData) {
//        String owner = getValueOrNull(pAttr.getNamedItem("owner").getNodeValue());
//        int access = Integer.parseInt(pAttr.getNamedItem("access").getNodeValue());
//        String name = getValueOrNull(pAttr.getNamedItem("name").getNodeValue());
//        String descriptor = getValueOrNull(pAttr.getNamedItem("descriptor").getNodeValue());
//        String signature = getValueOrNull(pAttr.getNamedItem("signature").getNodeValue());
//        Field newField = Field.create(owner, access, name, descriptor, signature);
//        pClassExecutionData.getFields().add(newField);
//    }

    private static void collectProgramVariableData(String pMethodName, NamedNodeMap pAttr, ClassExecutionData pClassExecutionData) {
        ProgramVariable programVariable = createProgramVariable(pAttr);
        if (pClassExecutionData.getVariablesCovered().get(pMethodName) == null) {
            Set<ProgramVariable> variablesCovered = new HashSet<>();
            variablesCovered.add(programVariable);
            pClassExecutionData.getVariablesCovered().put(pMethodName, variablesCovered);
        } else {
            pClassExecutionData.getVariablesCovered().get(pMethodName).add(programVariable);
        }
    }

    private static void collectDefUsePairData(String pMethodName, NamedNodeMap pAttr, ClassExecutionData pClassExecutionData) {
        DefUsePair defUsePair = createDefUsePair(pAttr);
        if (pClassExecutionData.getDefUsePairs().get(pMethodName) == null) {
            List<DefUsePair> defUsePairs = new ArrayList<>();
            defUsePairs.add(defUsePair);
            pClassExecutionData.getDefUsePairs().put(pMethodName, defUsePairs);
            pClassExecutionData.getDefUsePairsCovered().put(pMethodName, new HashMap<>());
        } else {
            pClassExecutionData.getDefUsePairs().get(pMethodName).add(defUsePair);
        }
    }

    private static ProgramVariable createProgramVariable(NamedNodeMap pAttr) {
        String owner = getValueOrNull(pAttr.getNamedItem("owner").getNodeValue());
        String name = getValueOrNull(pAttr.getNamedItem("name").getNodeValue());
        String type = getValueOrNull(pAttr.getNamedItem("descriptor").getNodeValue());
        String method = getValueOrNull(pAttr.getNamedItem("method").getNodeValue());
        int instructionIndex = Integer.parseInt(pAttr.getNamedItem("instructionIndex").getNodeValue());
        int lineNumber = Integer.parseInt(pAttr.getNamedItem("lineNumber").getNodeValue());
        boolean isReference = Boolean.parseBoolean(pAttr.getNamedItem("isReference").getNodeValue());
        boolean isDefinition = Boolean.parseBoolean(pAttr.getNamedItem("isDefinition").getNodeValue());
        return ProgramVariable.create(owner, name, type, method, instructionIndex, lineNumber, isReference, isDefinition);
    }

    private static DefUsePair createDefUsePair(NamedNodeMap pAttr) {
        String dOwner = getValueOrNull(pAttr.getNamedItem("dOwner").getNodeValue());
        String dName = getValueOrNull(pAttr.getNamedItem("dName").getNodeValue());
        String dType = getValueOrNull(pAttr.getNamedItem("dType").getNodeValue());
        String dMethod = getValueOrNull(pAttr.getNamedItem("dMethod").getNodeValue());
        int dIndex = Integer.parseInt(pAttr.getNamedItem("dIndex").getNodeValue());
        int dLineNumber = Integer.parseInt(pAttr.getNamedItem("dLineNumber").getNodeValue());
        boolean dIsReference = Boolean.parseBoolean(pAttr.getNamedItem("dIsReference").getNodeValue());
        boolean dIsDefinition = Boolean.parseBoolean(pAttr.getNamedItem("dIsDefinition").getNodeValue());
        ProgramVariable definition = ProgramVariable.create(dOwner, dName, dType, dMethod, dIndex, dLineNumber, dIsReference, dIsDefinition);

        String uOwner = getValueOrNull(pAttr.getNamedItem("uOwner").getNodeValue());
        String uName = getValueOrNull(pAttr.getNamedItem("uName").getNodeValue());
        String uType = getValueOrNull(pAttr.getNamedItem("uType").getNodeValue());
        String uMethod = getValueOrNull(pAttr.getNamedItem("uMethod").getNodeValue());
        int uIndex = Integer.parseInt(pAttr.getNamedItem("uIndex").getNodeValue());
        int uLineNumber = Integer.parseInt(pAttr.getNamedItem("uLineNumber").getNodeValue());
        boolean uIsReference = Boolean.parseBoolean(pAttr.getNamedItem("uIsReference").getNodeValue());
        boolean uIsDefinition = Boolean.parseBoolean(pAttr.getNamedItem("uIsDefinition").getNodeValue());
        ProgramVariable usage = ProgramVariable.create(uOwner, uName, uType, uMethod, uIndex, uLineNumber, uIsReference, uIsDefinition);
        return new DefUsePair(definition, usage);
    }

    private static String getValueOrNull(String pValue) {
        if (pValue.equals("")) {
            return null;
        }
        return pValue;
    }
}
