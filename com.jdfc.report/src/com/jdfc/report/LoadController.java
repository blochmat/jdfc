package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.data.Pair;
import com.jdfc.commons.utils.Files;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.InstanceVariable;
import com.jdfc.core.analysis.cfg.ProgramVariable;
import com.jdfc.core.analysis.data.ClassExecutionData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class LoadController {

    public static void loadDataFromXML(String workDir) {
        File dir = new File(workDir);
        Path baseDir = dir.toPath();
        String fileEnding = ".xml";

        // Loading data node structure
        CoverageDataStore.getInstance()
                .addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), baseDir, fileEnding);
        // Load simple xml files
        List<File> xmlFiles = Files.loadFilesFromDirRecursive(dir, ".xml");

        for (File xml : xmlFiles) {
            String relativePath = baseDir.relativize(xml.toPath()).toString();
            String relativePathWithoutType = relativePath.split("\\.")[0];
            ExecutionDataNode<ExecutionData> classExecutionDataNode = CoverageDataStore.getInstance().findClassDataNode(relativePathWithoutType);
            ClassExecutionData classExecutionData = (ClassExecutionData) classExecutionDataNode.getData();
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


    }

    private static void examineFileRecursive(Node pXMLNode, ClassExecutionData classExecutionData) {
        NodeList nodeList = pXMLNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attr = node.getAttributes();
            if (attr != null) {
                if (node.getNodeName().equals("instanceVariables")) {
                    NodeList instanceVariables = node.getChildNodes();
                    getInstanceVariableData(instanceVariables, classExecutionData);
                } else if (node.getNodeName().equals("methodRangeMap")){
                    NodeList methodRanges = node.getChildNodes();
                    getMethodRangeData(methodRanges, classExecutionData);
                } else if (node.getNodeName().equals("method")) {
                    // Number of iterations over method labels == methodCount
                    classExecutionData.setMethodCount(classExecutionData.getMethodCount() + 1);
                    examineFileRecursive(node, classExecutionData);
                } else {
                    // DefUsePair or DefUseCovered list
                    String methodName = node.getParentNode().getAttributes().getNamedItem("name").getNodeValue();
                    NodeList defUsePairs = node.getChildNodes();
                    String listName = node.getNodeName();
                    collectCoverageData(methodName, defUsePairs, listName, classExecutionData);
                }
            }
        }
    }

    private static void getMethodRangeData(NodeList pMethodRanges, ClassExecutionData pClassExecutionData) {
        for(int i = 0; i < pMethodRanges.getLength(); i++) {
            Node methodRange = pMethodRanges.item(i);
            NamedNodeMap attr = methodRange.getAttributes();
            if (attr != null) {
                String methodName = attr.getNamedItem("methodName").getNodeValue();
                int fst = Integer.parseInt(attr.getNamedItem("fst").getNodeValue());
                int snd = Integer.parseInt(attr.getNamedItem("snd").getNodeValue());

                pClassExecutionData.getMethodRangeMap().put(methodName, new Pair<>(fst, snd));
            }
        }
    }

    private static void getInstanceVariableData(NodeList pInstanceVariables, ClassExecutionData classExecutionData) {
        for(int i = 0; i < pInstanceVariables.getLength(); i++) {
            Node instanceVariable = pInstanceVariables.item(i);
            NamedNodeMap attr = instanceVariable.getAttributes();
            if (attr != null) {
                String owner = attr.getNamedItem("owner").getNodeValue();
                int access = Integer.parseInt(attr.getNamedItem("access").getNodeValue());
                String name = attr.getNamedItem("name").getNodeValue();
                String descriptor = attr.getNamedItem("descriptor").getNodeValue();
                String signature = attr.getNamedItem("signature").getNodeValue();
                int lineNumber = Integer.parseInt(attr.getNamedItem("lineNumber").getNodeValue());

                InstanceVariable newVar = new InstanceVariable(owner, access, name, descriptor, signature, lineNumber);
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
                        String owner = pairAttr.getNamedItem("owner").getNodeValue();
                        String name = pairAttr.getNamedItem("name").getNodeValue();
                        String type = pairAttr.getNamedItem("type").getNodeValue();
                        int definitionIndex = Integer.parseInt(pairAttr.getNamedItem("definitionIndex").getNodeValue());
                        int definitionLineNumber = Integer.parseInt(pairAttr.getNamedItem("definitionLineNumber").getNodeValue());
                        int usageIndex = Integer.parseInt(pairAttr.getNamedItem("usageIndex").getNodeValue());
                        int usageLineNumber = Integer.parseInt(pairAttr.getNamedItem("usageLineNumber").getNodeValue());
                        ProgramVariable definition = ProgramVariable.create(owner, name, type, definitionIndex, definitionLineNumber);
                        ProgramVariable usage = ProgramVariable.create(owner, name, type, usageIndex, usageLineNumber);
                        DefUsePair newPair = new DefUsePair(definition, usage);
                        classExecutionData.getDefUsePairs().get(methodName).add(newPair);
                    }
                }
                classExecutionData.setTotal(classExecutionData.getDefUsePairs().get(methodName).size());
                break;
            case "defUseCovered":
                classExecutionData.getDefUseCovered().put(methodName, new HashSet<>());
                for (int j = 0; j < nodeList.getLength(); j++) {
                    Node pair = nodeList.item(j);
                    NamedNodeMap pairAttr = pair.getAttributes();
                    if (pairAttr != null) {
                        String owner = pairAttr.getNamedItem("owner").getNodeValue();
                        String name = pairAttr.getNamedItem("name").getNodeValue();
                        String type = pairAttr.getNamedItem("type").getNodeValue();
                        int instructionIndex = Integer.parseInt(pairAttr.getNamedItem("instructionIndex").getNodeValue());
                        int lineNumber = Integer.parseInt(pairAttr.getNamedItem("lineNumber").getNodeValue());
                        ProgramVariable programVariable = ProgramVariable.create(owner, name, type, instructionIndex, lineNumber);
                        classExecutionData.getDefUseCovered().get(methodName).add(programVariable);
                    }
                }
                classExecutionData.computeCoverage();
                break;
            default:
                throw new IllegalArgumentException("Invalid Tag in XML!");
        }
    }
}
