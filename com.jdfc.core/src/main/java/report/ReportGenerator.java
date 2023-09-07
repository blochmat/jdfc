package report;

import data.ClassExecutionData;
import data.ExecutionData;
import data.ExecutionDataNode;
import data.singleton.CoverageDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import report.html.HTMLFactory;
import report.html.Resources;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportGenerator {

    private Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    private final File outputDir;
    private final File sourceDir;

    public ReportGenerator(String outputDirAbs, String sourceDirAbs) {
        this.outputDir = new File(outputDirAbs);
        this.sourceDir = new File(sourceDirAbs);
    }

    public void createHTMLReport() {
        ExecutionDataNode<ExecutionData> root = CoverageDataStore.getInstance().getRoot();
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

//    private void createPackageRelatedHTMLFilesRecursive(final HTMLFactory factory,
//                                                        final ExecutionDataNode<ExecutionData> node,
//                                                        final String filePathAbs) throws IOException {
//        logger.debug(String.format("createPackageRelatedHTMLFilesRecursive(<HTMLFactory>, <ExecutionDataNode>, %s)", filePathAbs));
//        Map<String, ExecutionDataNode<ExecutionData>> currentNodeChildren = node.getChildren();
//        Map<String, ExecutionDataNode<ExecutionData>> classExecutionDataNodeMap = new TreeMap<>();
//        File outputFolder = new File(filePathAbs);
//        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> childEntry : currentNodeChildren.entrySet()) {
//            if (childEntry.getValue().isLeaf()) {
//                classExecutionDataNodeMap.put(childEntry.getKey(), childEntry.getValue());
//                if (outputFolder.exists() || outputFolder.mkdir()) {
//                    // method overview
//                    factory.createClassOverviewHTML(childEntry.getKey(), childEntry.getValue().getData(), outputFolder);
//                    // class detail view
//                    factory.createClassSourceViewHTML(childEntry.getKey(), childEntry.getValue().getData(), outputFolder,
//                            sourceDir);
//                }
//            } else {
//                String nextPathName;
//                if (filePathAbs.equals(reportDir.toString())) {
//                    nextPathName = String.format("%s/%s", filePathAbs, childEntry.getKey());
//                } else {
//                    nextPathName = String.format("%s.%s", filePathAbs, childEntry.getKey());
//                }
//                createPackageRelatedHTMLFilesRecursive(factory, childEntry.getValue(), nextPathName);
//            }
//        }
//        if (!classExecutionDataNodeMap.isEmpty()) {
//            factory.createPkgIndexHTML(classExecutionDataNodeMap, outputFolder);
//        }
//    }

    private void createHTMLFiles(HTMLFactory factory) throws IOException {
        System.out.println("debug");
        for(Map.Entry<String, Map<String, ClassExecutionData>> packageEntry : CoverageDataStore.getInstance().getProjectData().entrySet()) {
            String packageAbs = String.join(File.separator, outputDir.getAbsolutePath(), packageEntry.getKey());
            File pkg = new File(packageAbs);
            if(pkg.exists() || pkg.mkdirs()) {
                for(Map.Entry<String, ClassExecutionData> classEntry : packageEntry.getValue().entrySet()) {
                    factory.createClassOverviewHTML(classEntry.getKey(), classEntry.getValue(), pkg);
                    factory.createClassSourceViewHTML(classEntry.getKey(), classEntry.getValue(), pkg, sourceDir);
                }
                factory.createPkgIndexHTML(pkg, packageEntry.getValue());
            } else {
                System.err.println("Directory could not be created: " + packageAbs);
            }
        }
    }

    private void createRootIndexHTML(final HTMLFactory pFactory, final File outputDir) throws IOException {
        pFactory.createRootIndexHTML(outputDir);
    }

    private Map<String, ExecutionDataNode<ExecutionData>> getClassContainingPackagesRecursive(
            final ExecutionDataNode<ExecutionData> pNode,
            final String pPackageName) {
        Map<String, ExecutionDataNode<ExecutionData>> currentNodeChildren = pNode.getChildren();
        Map<String, ExecutionDataNode<ExecutionData>> packageExecutionDataNodeMap = new HashMap<>();

        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> childEntry : currentNodeChildren.entrySet()) {
            if (childEntry.getValue().isLeaf()) {
                packageExecutionDataNodeMap.put(pPackageName, pNode);
            } else {
                String nextPathName;
                if (pPackageName.equals("")) {
                    nextPathName = childEntry.getKey();
                } else {
                    nextPathName = String.format("%s.%s", pPackageName, childEntry.getKey());
                }
                packageExecutionDataNodeMap = mergeMaps(packageExecutionDataNodeMap,
                        getClassContainingPackagesRecursive(childEntry.getValue(), nextPathName));
            }
        }
        return packageExecutionDataNodeMap;
    }

    private Map<String, ExecutionDataNode<ExecutionData>> mergeMaps(Map<String, ExecutionDataNode<ExecutionData>> map1,
                                                                    Map<String, ExecutionDataNode<ExecutionData>> map2) {
        return Stream.of(map1, map2)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}
