package com.jdfc.agent.rt.internal;

import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.CoverageDataStore;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.*;

public final class PreMain {

    private PreMain(){

    }

    public static void premain(final String options, final Instrumentation inst)
            throws Exception {
        File dir = new File(options);
        Path baseDir = dir.toPath();
        String fileEnding = ".class";
        CoverageDataStore.getInstance().addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), baseDir, fileEnding);
        printTree(CoverageDataStore.getInstance().getRoot());
        inst.addTransformer(new ClassTransformer());
    }

    // TODO: WRONG SPOT
    private static void printTree(ExecutionDataNode<ExecutionData> executionDataNode) {
        if (!(executionDataNode.getChildren().size() == 0)) {
            PrettyPrintMap<String, ExecutionDataNode<ExecutionData>> prettyPrintMap = new PrettyPrintMap<>(executionDataNode.getChildren());
            for(Map.Entry<String, ExecutionDataNode<ExecutionData>> childNode : executionDataNode.getChildren().entrySet()) {
                printTree(childNode.getValue());
            }
        }
    }
}
