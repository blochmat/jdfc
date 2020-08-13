package com.jdfc.maven;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Locale;


@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ReportMojo extends AbstractMavenReport {

    @Parameter(defaultValue = "${project.reporting.outputDirectory}/jdfc")
    private File outputDirectory;

    @Parameter(property = "jdfc.dataFile", defaultValue = "${project.build.directory}/jdfc.exec")
    private File dataFile;

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

        File xmlFile = new File(System.getProperty("user.dir")+"/target/output.xml");
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            assert doc != null;
            Element root = doc.getDocumentElement();

            NodeList nodeList = root.getChildNodes();
            System.out.println("-----------------------------");
            for(int i = 1; i < nodeList.getLength(); i++){
                Node node = nodeList.item(i);
                String.format("%s, %s, %s, %s",
                        node.getLocalName(), node.getNodeType(), node.getNamespaceURI());

                NodeList methodList = nodeList.item(i).getChildNodes();
                System.out.println("--------------A---------------");
                for(int j = 1; j < methodList.getLength(); j++) {
                    System.out.println(methodList.item(j).getNodeName());

                }
                System.out.println("--------------E---------------");
            }
            System.out.println("-----------------------------");
        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void buildHTMLFile(NodeList list) {
        System.out.println(list.toString());
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
