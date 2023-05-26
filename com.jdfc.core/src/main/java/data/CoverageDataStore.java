package data;

import cfg.CFG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * A storage singleton for all class data required for the analysis. A tree structure of {@code ExecutionDataNode}
 * instance represent the project structure of the project under test
 */
public class CoverageDataStore {
    private final Logger logger = LoggerFactory.getLogger(CoverageDataStore.class);

    private final ExecutionDataNode<ExecutionData> root;
    private final List<String> testedClassList;
    private final List<String> untestedClassList;
    private String projectDirStr;
    private String buildDirStr;
    private String classesBuildDirStr;
    private List<String> srcDirStrList;

    private CoverageDataStore() {
        // TODO: Maybe add src/main/java here somehow
        ExecutionData executionData = new ExecutionData("");
        this.root = new ExecutionDataNode<>(executionData);
        this.testedClassList = new ArrayList<>();
        this.untestedClassList = new ArrayList<>();
    }

    private static class Container {
        static CoverageDataStore instance = new CoverageDataStore();
    }

    public static CoverageDataStore getInstance() {
        return Container.instance;
    }

    public ExecutionDataNode<ExecutionData> getRoot() {
        return root;
    }

    public List<String> getTestedClassList() {
        return testedClassList;
    }

    public List<String> getUntestedClassList() {
        return untestedClassList;
    }

    public String getProjectDirStr() {
        return projectDirStr;
    }

    public String getBuildDirStr() {
        return buildDirStr;
    }

    public String getClassesBuildDirStr() {
        return classesBuildDirStr;
    }

    public List<String> getSrcDirStrList() {
        return srcDirStrList;
    }

    public void saveProjectInfo(String projectDirStr,
                                String buildDirStr,
                                String classesBuildDirStr,
                                List<String> srcDirStrList) {
       this.projectDirStr = projectDirStr;
       this.buildDirStr = buildDirStr;
       this.classesBuildDirStr = classesBuildDirStr;
       this.srcDirStrList = srcDirStrList;
    }

    public static void invokeCoverageTracker(final String pClassName,
                                             final String pInternalMethodName,
                                             final int pVarIndex,
                                             final int pInsnIndex,
                                             final int pLineNumber,
                                             final int pOpcode) {
        CoverageTracker.getInstance().addLocalVarCoveredEntry(pClassName, pInternalMethodName, pVarIndex, pInsnIndex,
                pLineNumber, pOpcode);
    }

    public void exportCoverageData() {
        // TODO: Insert export method call here
        try {
            CoverageDataExport.dumpCoverageDataToFile();
        } catch (ParserConfigurationException | TransformerException e) {
            throw new RuntimeException(e);
        }

        for(String className : testedClassList) {
            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
            try {
                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        }

        for(String className : untestedClassList) {
            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
            try {
                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        }
    }

    public void finishClassExecutionDataSetup(final ClassExecutionData pClassExecutionData,
                                              final Map<String, CFG> pMethodCFGs) {
        pClassExecutionData.setMethodCFGs(pMethodCFGs);
        pClassExecutionData.initializeDefUseLists();
//        pClassExecutionData.insertAdditionalDefs();
        pClassExecutionData.calculateReachingDefs();
        pClassExecutionData.calculateIntraProceduralDefUsePairs();
//        pClassExecutionData.setupInterProceduralMatches();
    }

    public ExecutionDataNode<ExecutionData> findClassDataNode(String pClassName) {
        ArrayList<String> nodePath = new ArrayList<>(Arrays.asList(pClassName.replace(File.separator, "/").split("/")));

        // root path
        if (nodePath.size() == 1) {
            nodePath.add(0, "default");
        }

        return root.getChildDataRecursive(nodePath);
    }

    public void addNodesFromDirRecursive(File pFile,
                                         ExecutionDataNode<ExecutionData> pExecutionDataNode,
                                         Path pBaseDir,
                                         String suffix) {
        File[] fileList = Objects.requireNonNull(pFile.listFiles());
        boolean isClassDir = Arrays.stream(fileList).anyMatch(x -> x.getName().contains(suffix));
        if (pExecutionDataNode.isRoot() && isClassDir) {
            pExecutionDataNode.addChild("default", this.root.getData());
        }
        for (File f : fileList) {
            String fqn = createFqn(pExecutionDataNode, f.getName());
            if (f.isDirectory() && !f.getName().equals("META-INF")) {
                ExecutionData pkgData = new ExecutionData(fqn);
                ExecutionDataNode<ExecutionData> newPkgExecutionDataNode = new ExecutionDataNode<>(pkgData);
                pExecutionDataNode.addChild(f.getName(), newPkgExecutionDataNode);
                addNodesFromDirRecursive(f, newPkgExecutionDataNode, pBaseDir, suffix);
            } else if (f.isFile() && f.getName().endsWith(suffix)) {
                String relativePathWithType = pBaseDir.relativize(f.toPath()).toString();
                String relativePath = relativePathWithType.split("\\.")[0].replace(File.separator, "/");
                // Add className to classList of storage. Thereby we determine, if class needs to be instrumented
                untestedClassList.add(relativePath);
                String nameWithoutType = f.getName().split("\\.")[0];
                ClassExecutionData classNodeData = new ClassExecutionData(fqn, relativePath);
                if (pExecutionDataNode.isRoot()) {
                    pExecutionDataNode.getChildren().get("default").addChild(nameWithoutType, classNodeData);
                } else {
                    pExecutionDataNode.addChild(nameWithoutType, classNodeData);
                }
            }
        }
    }

    private String createFqn(ExecutionDataNode<ExecutionData> node, String childName) {
        if (node.isRoot()) {
            return childName;
        } else {
            return String.format("%s.%s", node.getData().getFqn(), childName);
        }
    }
}
