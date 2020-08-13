package com.jdfc.maven;

import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.ProgramVariable;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;


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
        File xmlFile = new File(System.getProperty("user.dir") + "/target/output.xml");
        try {
            buildHTMLFile(parseXMLFile(xmlFile));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }

    }

    private List<DUPairInformation> parseXMLFile(File file) throws ParserConfigurationException, IOException, SAXException {
        List<DUPairInformation> duPairList = new ArrayList<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);

        if (doc == null) {
            throw new FileNotFoundException("Could not find XML file to parse.");
        }

        Element root = doc.getDocumentElement();
        NodeList nodeList = root.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node method = nodeList.item(i);
            NamedNodeMap methodAttr = method.getAttributes();

            if (methodAttr == null) {
                continue;
            }

            Node methodName = methodAttr.getNamedItem("name");
            NodeList pairList = method.getChildNodes();

            for (int j = 0; j < pairList.getLength(); j++) {
                Node pair = pairList.item(j);
                NamedNodeMap pairAttr = pair.getAttributes();

                if (pairAttr == null) {
                    continue;
                }

                Node pairName = pairAttr.getNamedItem("name");
                Node pairType = pairAttr.getNamedItem("type");
                Node pairDefIndex = pairAttr.getNamedItem("defIndex");
                Node pairUseIndex = pairAttr.getNamedItem("useIndex");
                Node pairCovered = pairAttr.getNamedItem("covered");
                DUPairInformation duPairInformation = new DUPairInformation(
                        methodName.getNodeValue(),
                        pairName.getNodeValue(),
                        Integer.parseInt(pairDefIndex.getNodeValue()),
                        Integer.parseInt(pairUseIndex.getNodeValue()),
                        pairType.getNodeValue(),
                        Boolean.parseBoolean(pairCovered.getNodeValue()));
                duPairList.add(duPairInformation);
            }
        }
        return duPairList;
    }

    private void buildHTMLFile(List<DUPairInformation> list) {
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
