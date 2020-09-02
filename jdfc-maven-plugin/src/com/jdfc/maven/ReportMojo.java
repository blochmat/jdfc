package com.jdfc.maven;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;


@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ReportMojo extends AbstractMavenReport {

    @Parameter(defaultValue = "${project.reporting.outputDirectory}/jdfc")
    private File outputDirectory;

    @Override
    protected Renderer getSiteRenderer() {
        return null;
    }

    @Override
    protected String getOutputDirectory() {
        return null;
    }

    @Override
    protected MavenProject getProject() {
        return null;
    }

    @Override
    public void execute() {
        try {
            executeReport(Locale.getDefault());
        } catch (MavenReportException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
//        File jdfcDir = new File(System.getProperty("user.dir") + "/target/jdfc");
//        try {
//            ExecutionDataNode<ExecutionData> root = convertXMLToExecutionData(jdfcDir);
//            createReport(root);
//        } catch (SAXException | IOException | ParserConfigurationException e) {
//            e.printStackTrace();
//        }
    }

    // return executiondatatree with all execution data from xml file
    // first package node always is the root of the data tree
    private ExecutionDataNode<ExecutionData> convertXMLToExecutionData(File file) throws ParserConfigurationException, IOException, SAXException {
//        PackageExecutionData packageExecutionData = new PackageExecutionData();
//        ExecutionDataNode<ExecutionData> packageNode = new ExecutionDataNode<>(packageExecutionData);
//        if (file.isDirectory()) {
//            File[] fileList = Objects.requireNonNull(file.listFiles());
//            for (File f : fileList) {
//                packageNode.addChild(f.getName(), convertXMLToExecutionData(f));
//            }
//        } else {
//            ClassExecutionData classExecutionData = new ClassExecutionData();
//            ExecutionDataNode<ExecutionData> classNode = new ExecutionDataNode<>(classExecutionData);
//            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
//
//            if (doc == null) {
//                throw new FileNotFoundException("Could not find XML file to parse.");
//            }
//
//            Element root = doc.getDocumentElement();
//            NodeList nodeList = root.getChildNodes();
//
//            for (int i = 0; i < nodeList.getLength(); i++) {
//                Node node = nodeList.item(i);
//                NamedNodeMap attr = node.getAttributes();
//                if (attr != null) {
//                    dataTree.getRoot().getChildren().put(node.getNodeName(), examineElement(node, attr));
//                }
//            }
//            return dataTree;
//        }
//        return packageNode;
        return null;
    }

    // recursive function to create nodes for executionDataTree
    private ExecutionDataNode<ExecutionData> examineElement(Node node, NamedNodeMap attr) {

//        NodeList nodeChildNodes = node.getChildNodes();
//
//        ExecutionDataNodeType type = determineType(node.getAttributes().getNamedItem("tagType").getNodeValue());
//
//        // is package, class, method
//        if (type != null) {
//            PCMNode pcmNode = new PCMNode(type);
//            for (int i = 0; i < nodeChildNodes.getLength(); i++) {
//                Node childNode = nodeChildNodes.item(i);
//                NamedNodeMap cAttr = childNode.getAttributes();
//                if (cAttr != null) {
//                    pcmNode.getChildren().put(childNode.getNodeName(), examineElement(childNode, cAttr));
//                }
//            }
//            return pcmNode;
//        } else {
//            VariableNode variableNode = new VariableNode(node.getNodeValue(),
//                    attr.getNamedItem("type").getNodeValue());
//            for (int i = 0; i < nodeChildNodes.getLength(); i++) {
//                Node childNode = nodeChildNodes.item(i);
//                NamedNodeMap cAttr = childNode.getAttributes();
//                if (cAttr != null) {
//                    CoverageInformation info =
//                            new CoverageInformation(Integer.parseInt(cAttr.getNamedItem("defIndex").getNodeValue()),
//                                    Integer.parseInt(cAttr.getNamedItem("useIndex").getNodeValue()),
//                                    Boolean.parseBoolean(cAttr.getNamedItem("covered").getNodeValue()));
//                    variableNode.getCoverageInformation().add(info);
//                }
//            }
//            return variableNode;
//        }
        return null;
    }

    private void createReport(ExecutionDataNode<ExecutionData> node) {
        try {
            Writer writer = new FileWriter(new File(System.getProperty("user.dir") + "/target/htmlFile.html"));
            writeHTMLRecursive(writer, node);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHTMLRecursive(Writer pWriter, ExecutionDataNode<ExecutionData> pNode) throws IOException {
        pWriter.write(String.format("<!DOCTYPE html><html><head>%s</head><body>%s</body></html>",
                writeHeader(pWriter, pNode),
                writeBodyRecursive(pWriter, pNode)));
    }

    // Content and coverage information
    private String writeBodyRecursive(Writer pWriter, ExecutionDataNode pNode) {
        if (pNode.getData() instanceof ClassExecutionData) {
            // return table entry with columns (depending on type of node)
        } else {
            // return class view with marked entries
        }
        return "This is the body";
    }


    // Style and header title
    private String writeHeader(Writer pWriter, ExecutionDataNode root) {
        return "This is a Header";
    }

    @Override
    public String getOutputName() {
        return null;
    }

    @Override
    public String getName(Locale locale) {
        return null;
    }

    @Override
    public String getDescription(Locale locale) {
        return null;
    }
}
