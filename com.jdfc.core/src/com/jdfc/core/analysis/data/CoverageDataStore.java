package com.jdfc.core.analysis.data;

import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.ifg.CFG;
import com.jdfc.commons.data.ExecutionData;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/** A storage singleton for package, class and finally method {@link CFG}s. */
public class CoverageDataStore {

    private static CoverageDataStore singleton;
    private final ExecutionDataNode<ExecutionData> root;
    private final List<String> classList;

    private CoverageDataStore(){
        ExecutionData executionData = new ExecutionData();
        this.root = new ExecutionDataNode<>(executionData);
        this.classList = new ArrayList<>();
    }

    public static synchronized CoverageDataStore getInstance(){
        if(singleton == null) {
            singleton =  new CoverageDataStore();
        }
        return singleton;
    }

    public ExecutionDataNode<ExecutionData> getRoot() {
        return root;
    }

    public List<String> getClassList(){
        return classList;
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

    public static void invokeCoverageTracker(final String pClassName,
                                             final String pOwner,
                                             final String pInternalMethodName,
                                             final String pVarName,
                                             final String pVarDesc,
                                             final int pInsnIndex,
                                             final int pLineNumber,
                                             final int pOpcode) {
        CoverageTracker.getInstance().addInstanceVarCoveredEntry(pClassName, pOwner, pInternalMethodName, pVarName, pVarDesc,
                pInsnIndex, pLineNumber, pOpcode);
    }

    public static void invokeCoverageTracker(final String pClassName) {
        CoverageTracker.getInstance().dumpClassExecutionDataToFile(pClassName);
    }

    public void finishClassExecutionDataSetup(final ClassExecutionData pClassExecutionData,
                                              final Map<String, CFG> pMethodCFGs){
        pClassExecutionData.setMethodCFGs(pMethodCFGs);
        pClassExecutionData.initializeDefUseLists();
        pClassExecutionData.insertAdditionalDefs();
        pClassExecutionData.calculateReachingDefs();
        pClassExecutionData.calculateIntraProceduralDefUsePairs();
        pClassExecutionData.setupInterProceduralMatches();
    }

    public ExecutionDataNode<ExecutionData> findClassDataNode(String pClassName) {
        ArrayList<String> nodePath = new ArrayList<>(Arrays.asList(pClassName.split("/")));

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
        if(pExecutionDataNode.isRoot()){
            ExecutionData rootClassData = new ExecutionData();
            pExecutionDataNode.addChild("default", rootClassData);
        }
        for (File f : fileList){
            if(f.isDirectory()) {
                ExecutionData pkgData = new ExecutionData();
                ExecutionDataNode<ExecutionData> newPkgExecutionDataNode = new ExecutionDataNode<>(pkgData);
                pExecutionDataNode.addChild(f.getName(), newPkgExecutionDataNode);
                addNodesFromDirRecursive(f, newPkgExecutionDataNode, pBaseDir, suffix);
            } else if (f.isFile() && f.getName().endsWith(suffix)) {
                String relativePathWithType = pBaseDir.relativize(f.toPath()).toString();
                String relativePath = relativePathWithType.split("\\.")[0];
                // Add className to classList of storage. Thereby we determine, if class needs to be instrumented
                classList.add(relativePath);
                String nameWithoutType = f.getName().split("\\.")[0];
                ClassExecutionData classNodeData = new ClassExecutionData(relativePath);
                if(pExecutionDataNode.isRoot()){
                    pExecutionDataNode.getChildren().get("default").addChild(nameWithoutType, classNodeData);
                } else {
                    pExecutionDataNode.addChild(nameWithoutType, classNodeData);
                }
            }
        }
    }
}
