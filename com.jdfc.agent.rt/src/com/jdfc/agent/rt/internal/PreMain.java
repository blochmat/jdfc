package com.jdfc.agent.rt.internal;

import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;
import com.jdfc.core.analysis.internal.data.PackageExecutionData;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class PreMain {

    private PreMain(){

    }

    public static void premain(final String options, final Instrumentation inst)
            throws Exception {
        Path baseDir = Paths.get(options).toAbsolutePath();
        File f = new File(options);
        addNodes(f, CoverageDataStore.getInstance().getRoot(), baseDir);
        printTree(CoverageDataStore.getInstance().getRoot());
        inst.addTransformer(new ClassTransformer());
    }

    private static void addNodes(File pFile, ExecutionDataNode<ExecutionData> pExecutionDataNode, Path pBaseDir) {
        File[] fileList = Objects.requireNonNull(pFile.listFiles());

        for (File f : fileList){
            if(f.isDirectory()) {
                PackageExecutionData pkgData = new PackageExecutionData();
                ExecutionDataNode<ExecutionData> newPkgExecutionDataNode = new ExecutionDataNode<>(pkgData);
                pExecutionDataNode.addChild(f.getName(), newPkgExecutionDataNode);
                addNodes(f, newPkgExecutionDataNode, pBaseDir);
            } else if (f.isFile() && f.getName().endsWith(".class")) {
                String relativePath = pBaseDir.relativize(f.toPath()).toString();
                String relativePathWithoutType = relativePath.split("\\.")[0];
                // Add className to classList of storage. Thereby we determine, if class needs to be instrumented
                CoverageDataStore.getInstance().getClassList().add(relativePathWithoutType);

                String nameWithoutType = f.getName().split("\\.")[0];
                ClassExecutionData classNodeData = new ClassExecutionData();
                pExecutionDataNode.addChild(nameWithoutType, classNodeData);
            }
        }
    }

    private static void printTree(ExecutionDataNode<ExecutionData> executionDataNode) {
        if (!(executionDataNode.getChildren().size() == 0)) {
            PrettyPrintMap<String, ExecutionDataNode<ExecutionData>> prettyPrintMap = new PrettyPrintMap<>(executionDataNode.getChildren());
            for(Map.Entry<String, ExecutionDataNode<ExecutionData>> childNode : executionDataNode.getChildren().entrySet()) {
                printTree(childNode.getValue());
            }
        }
    }
}
