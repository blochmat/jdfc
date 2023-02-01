package data;

import ifg.CFG;

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

    private final ExecutionDataNode<ExecutionData> root;
    private final List<String> classList;

    private CoverageDataStore() {
        ExecutionData executionData = new ExecutionData();
        this.root = new ExecutionDataNode<>(executionData);
        this.classList = new ArrayList<>();
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

    public List<String> getClassList() {
        return classList;
    }

    public static void invokeCoverageTracker(final String pClassName,
                                             final String pInternalMethodName,
                                             final int pVarIndex,
                                             final int pInsnIndex,
                                             final int pLineNumber,
                                             final int pOpcode) {
        System.err.println("[DEBUG] invokeCoverageTracker");
        CoverageTracker.getInstance().addLocalVarCoveredEntry(pClassName, pInternalMethodName, pVarIndex, pInsnIndex,
                pLineNumber, pOpcode);
    }

    public void exportCoverageData() {
        System.err.println("[DEBUG] exportCoverageData");
        for(String className : classList) {
            System.err.println("[DEBUG] " + className);
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
        pClassExecutionData.insertAdditionalDefs();
        pClassExecutionData.calculateReachingDefs();
        pClassExecutionData.calculateIntraProceduralDefUsePairs();
        pClassExecutionData.setupInterProceduralMatches();
    }

    public ExecutionDataNode<ExecutionData> findClassDataNode(String pClassName) {
        System.err.println("[DEBUG] findClassDataNode " + pClassName);
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
        System.err.println("[DEBUG] addNodesFromDirRecursive");
        File[] fileList = Objects.requireNonNull(pFile.listFiles());
        System.err.println("[DEBUG] addNodesFromDirRecursive 2");
        boolean isClassDir = Arrays.stream(fileList).anyMatch(x -> x.getName().contains(suffix));
        if (pExecutionDataNode.isRoot() && isClassDir) {
            ExecutionData rootClassData = new ExecutionData();
            pExecutionDataNode.addChild("default", rootClassData);
        }
        for (File f : fileList) {
            System.err.println("[DEBUG] "+ f.getName());
            if (f.isDirectory() && !f.getName().equals("META-INF")) {
                ExecutionData pkgData = new ExecutionData();
                ExecutionDataNode<ExecutionData> newPkgExecutionDataNode = new ExecutionDataNode<>(pkgData);
                pExecutionDataNode.addChild(f.getName(), newPkgExecutionDataNode);
                addNodesFromDirRecursive(f, newPkgExecutionDataNode, pBaseDir, suffix);
            } else if (f.isFile() && f.getName().endsWith(suffix)) {
                String relativePathWithType = pBaseDir.relativize(f.toPath()).toString();
                String relativePath = relativePathWithType.split("\\.")[0].replace(File.separator, "/");
                // Add className to classList of storage. Thereby we determine, if class needs to be instrumented
                classList.add(relativePath);
                String nameWithoutType = f.getName().split("\\.")[0];
                ClassExecutionData classNodeData = new ClassExecutionData(relativePath);
                if (pExecutionDataNode.isRoot()) {
                    pExecutionDataNode.getChildren().get("default").addChild(nameWithoutType, classNodeData);
                } else {
                    pExecutionDataNode.addChild(nameWithoutType, classNodeData);
                }
            }
        }
    }
}