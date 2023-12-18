package report;

import data.ClassData;
import data.PackageData;
import data.ProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import report.html.HTMLFactory;
import report.html.Resources;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class HTMLReportGenerator {

    private final Logger logger = LoggerFactory.getLogger(HTMLReportGenerator.class);
    private final File outputDir;
    private final File sourceDir;

    public HTMLReportGenerator(String outputDirAbs, String sourceDirAbs) {
        this.outputDir = new File(outputDirAbs);
        this.sourceDir = new File(sourceDirAbs);
    }

    public void create() {
        if (outputDir.exists() || outputDir.mkdirs()) {
            try {
                Resources resources = new Resources(outputDir);
                HTMLFactory factory = new HTMLFactory(resources, outputDir);
                createHTMLFiles(factory);
                createRootIndexHTML(factory, outputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createHTMLFiles(HTMLFactory factory) throws IOException {
        for(Map.Entry<String, PackageData> packageEntry : ProjectData.getInstance().getPackageDataMap().entrySet()) {
            String packageAbs = String.join(File.separator, outputDir.getAbsolutePath(), packageEntry.getKey());
            File pkg = new File(packageAbs);
            if(pkg.exists() || pkg.mkdirs()) {
                for(ClassData classData : packageEntry.getValue().getClassDataFromStore().values()) {
                    factory.createClassOverviewHTML(classData.getClassMetaData().getFqn(), classData, pkg);
                    factory.createClassSourceViewHTML(classData.getClassMetaData().getFqn(), classData, pkg, sourceDir);
                }
                factory.createPkgIndexHTML(pkg, packageEntry.getValue().getClassDataFromStore());
            } else {
                System.err.println("Directory could not be created: " + packageAbs);
            }
        }
    }

    private void createRootIndexHTML(final HTMLFactory pFactory, final File outputDir) throws IOException {
        pFactory.createRootIndexHTML(outputDir);
    }
}
