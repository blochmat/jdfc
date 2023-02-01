package report;

import data.CoverageDataStore;
import data.ExecutionData;
import data.ExecutionDataNode;
import report.html.HTMLFactory;
import report.html.Resources;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportGenerator {

    private final File reportDir;
    private final File sourceDir;

    public ReportGenerator(String pReportDir, String pSourceDir) {
        this.reportDir = new File(pReportDir);
        this.sourceDir = new File(pSourceDir);
    }

    public void createReport() {
        ExecutionDataNode<ExecutionData> root = CoverageDataStore.getInstance().getRoot();
        if (reportDir.exists() || reportDir.mkdir()) {
            try {
                System.err.println("[DEBUG] reportDir = " + reportDir);
                Resources resources = new Resources(reportDir);
                HTMLFactory factory = new HTMLFactory(resources, reportDir);
                createPackageRelatedHTMLFilesRecursive(factory, root, reportDir.toString());
                createInitialIndexFile(factory, root, reportDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createPackageRelatedHTMLFilesRecursive(final HTMLFactory pFactory,
                                                        final ExecutionDataNode<ExecutionData> pNode,
                                                        final String pPathName) throws IOException {
        Map<String, ExecutionDataNode<ExecutionData>> currentNodeChildren = pNode.getChildren();
        Map<String, ExecutionDataNode<ExecutionData>> classExecutionDataNodeMap = new TreeMap<>();
        File outputFolder = new File(pPathName);
        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> childEntry : currentNodeChildren.entrySet()) {
            if (childEntry.getValue().isLeaf()) {
                classExecutionDataNodeMap.put(childEntry.getKey(), childEntry.getValue());
                if (outputFolder.exists() || outputFolder.mkdir()) {
                    // method overview
                    pFactory.createClassOverview(childEntry.getKey(), childEntry.getValue().getData(), outputFolder);
                    // class detail view
                    pFactory.createClassSourceView(childEntry.getKey(), childEntry.getValue().getData(), outputFolder,
                            sourceDir);
                }
            } else {
                String nextPathName;
                if (pPathName.equals(reportDir.toString())) {
                    nextPathName = String.format("%s/%s", pPathName, childEntry.getKey());
                } else {
                    nextPathName = String.format("%s.%s", pPathName, childEntry.getKey());
                }
                createPackageRelatedHTMLFilesRecursive(pFactory, childEntry.getValue(), nextPathName);
            }
        }
        if (!classExecutionDataNodeMap.isEmpty()) {
            pFactory.createIndex(classExecutionDataNodeMap, outputFolder);
        }
    }

    private void createInitialIndexFile(final HTMLFactory pFactory,
                                        final ExecutionDataNode<ExecutionData> pRoot,
                                        final File pReportDir) throws IOException {
        Map<String, ExecutionDataNode<ExecutionData>> packageExecutionDataNodeMap =
                getClassContainingPackagesRecursive(pRoot, "");
        pFactory.createIndex(packageExecutionDataNodeMap, pReportDir);
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
