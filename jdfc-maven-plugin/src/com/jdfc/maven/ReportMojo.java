package com.jdfc.maven;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.utils.Files;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.ProgramVariable;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;
import com.jdfc.core.analysis.internal.data.PackageExecutionData;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Path;
import java.util.*;


@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ReportMojo extends AbstractMavenReport {

    @Parameter(defaultValue = "${project.reporting.outputDirectory}/jdfc")
    private File outputDirectory;

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

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
        return this.project;
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
        final String target = getProject().getBuild().getDirectory();
        final String workDir = String.format("%s/jdfc", target);
        loadDataFromXML(workDir);
        debugPrintChildren(CoverageDataStore.getInstance().getRoot(), 1);
        createReport(workDir);
    }

    private void debugPrintChildren(ExecutionDataNode<ExecutionData> pNode, int indent) {
        if (pNode.isRoot()){
            PackageExecutionData rootData = (PackageExecutionData) pNode.getData();
            String root = String.format("root %s %s %s %s", rootData.getMethodCount(), rootData.getTotal(), rootData.getCovered(), rootData.getMissed());
            System.out.println(root);
        }

        Map<String, ExecutionDataNode<ExecutionData>> map = pNode.getChildren();
        String strip = "";
        for (int i = 0; i < indent; i++) {
            strip = strip.concat("- ");
        }
        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : map.entrySet()) {
            ExecutionData data = entry.getValue().getData();
            String str = String.format("%s%s %s %s %s %s", strip,
                    entry.getKey(), data.getMethodCount(), data.getTotal(), data.getCovered(), data.getMissed());
            System.out.println(str);
//            if (data instanceof ClassExecutionData) {
//                PrettyPrintMap<String, List<DefUsePair>> defUse =
//                        new PrettyPrintMap<>(((ClassExecutionData) data).getDefUsePairs());
//                PrettyPrintMap<String, Set<ProgramVariable>> covered =
//                        new PrettyPrintMap<>(((ClassExecutionData) data).getDefUseCovered());
//                System.out.println(strip + defUse.toString());
//                System.out.println(strip + covered);
//            }
            debugPrintChildren(entry.getValue(), indent + 1);
        }
    }

    private void loadDataFromXML(String workDir) {
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
                classExecutionDataNode.aggregateDataToParents();
            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    private void examineFileRecursive(Node pXMLNode, ClassExecutionData classExecutionData) {
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

    private void collectCoverageData(String methodName, NodeList nodeList, String list, ClassExecutionData classExecutionData) {
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
                        int usageIndex = Integer.parseInt(pairAttr.getNamedItem("usageIndex").getNodeValue());
                        ProgramVariable definition = new ProgramVariable(name, type, definitionIndex);
                        ProgramVariable usage = new ProgramVariable(name, type, usageIndex);
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
                        ProgramVariable programVariable = new ProgramVariable(name, type, instructionIndex);
                        classExecutionData.getDefUseCovered().get(methodName).add(programVariable);
                    }
                }
                classExecutionData.computeCoverage();
                break;
            default:
                throw new IllegalArgumentException("Invalid Tag in XML!");
        }
    }

    // TODO: Create HTML Report
    private void createReport(String workDir) {
        ExecutionDataNode<ExecutionData> root = CoverageDataStore.getInstance().getRoot();
        File index = new File(String.format("%s/index.html", workDir));
        File something = new File(String.format("%s/something.html", workDir));

        try {
            Writer writer = new FileWriter(index);
            writeHTMLRecursive(writer, root);
            writer.close();
            Writer someWriter = new FileWriter(something);
            someWriter.write("Its something!");
            someWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHTMLRecursive(Writer pWriter, ExecutionDataNode<ExecutionData> pNode) throws IOException {
        pWriter.write(String.format("<!DOCTYPE html><html><head>%s</head><body>%s</body></html>",
                writeHeader(pWriter, pNode),
                writeBodyRecursive(pNode)));
    }

    // Content and coverage information
    private String writeBodyRecursive(ExecutionDataNode<ExecutionData> pNode) {
        String str = "";
        if (pNode.getData() instanceof ClassExecutionData) {
            // return class view with marked entries
        } else {
            // create Table view with entry for every child and link to the respective html
            Map<String, ExecutionDataNode<ExecutionData>> children = pNode.getChildren();
            for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : children.entrySet()) {
                str = str.concat(String.format("<h1>%s</h1>\n", entry.getKey()));
            }
        }
        return str;
    }


    // Style and header title
    private String writeHeader(Writer pWriter, ExecutionDataNode<ExecutionData> root) {
        return "<a href=\"something.html\">Some Link</a>";
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
