package com.jdfc.report;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.utils.Files;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.cfg.DefUsePair;
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
        CoverageDataStore.getInstance()
                .addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), baseDir, fileEnding);
        List<File> xmlFiles = Files.loadFilesFromDirRecursive(dir, ".xml");

        for (File xml : xmlFiles) {
            String relativePath = baseDir.relativize(xml.toPath()).toString();
            String relativePathWithoutType = relativePath.split("\\.")[0];
            ExecutionDataNode<ExecutionData> classExecutionDataNode = CoverageDataStore.getInstance().findClassDataNode(relativePathWithoutType);
            ClassExecutionData classExecutionData = (ClassExecutionData) classExecutionDataNode.getData();
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
                Node rootTag = doc.getFirstChild();
                classExecutionData.setDefUsePairs(new TreeMap<>());
                classExecutionData.setDefUseCovered(new HashMap<>());
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
                if (node.getNodeName().equals("method")) {
                    classExecutionData.setMethodCount(classExecutionData.getMethodCount() + 1);
                    examineFileRecursive(node, classExecutionData);
                } else {
                    String methodName = node.getParentNode().getAttributes().getNamedItem("name").getNodeValue();
                    NodeList defUsePairs = node.getChildNodes();
                    String listName = node.getNodeName();
                    collectCoverageData(methodName, defUsePairs, listName, classExecutionData);
                }
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
                        String name = pairAttr.getNamedItem("name").getNodeValue();
                        String type = pairAttr.getNamedItem("type").getNodeValue();
                        int definitionIndex = Integer.parseInt(pairAttr.getNamedItem("definitionIndex").getNodeValue());
                        int definitionLineNumber = Integer.parseInt(pairAttr.getNamedItem("definitionLineNumber").getNodeValue());
                        int usageIndex = Integer.parseInt(pairAttr.getNamedItem("usageIndex").getNodeValue());
                        int usageLineNumber = Integer.parseInt(pairAttr.getNamedItem("usageLineNumber").getNodeValue());
                        ProgramVariable definition = new ProgramVariable(name, type, definitionIndex, definitionLineNumber);
                        ProgramVariable usage = new ProgramVariable(name, type, usageIndex, usageLineNumber);
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
                        String name = pairAttr.getNamedItem("name").getNodeValue();
                        String type = pairAttr.getNamedItem("type").getNodeValue();
                        int instructionIndex = Integer.parseInt(pairAttr.getNamedItem("instructionIndex").getNodeValue());
                        int lineNumber = Integer.parseInt(pairAttr.getNamedItem("lineNumber").getNodeValue());
                        ProgramVariable programVariable = new ProgramVariable(name, type, instructionIndex, lineNumber);
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
