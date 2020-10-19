package com.jdfc.agent;

import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.commons.data.ExecutionData;
import com.jdfc.core.analysis.data.CoverageDataStore;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.*;

public final class Agent {

    private Agent(){

    }

    public static void premain(final String options, final Instrumentation inst)
            throws Exception {
        File dir = new File(options);
        Path baseDir = dir.toPath();
        String fileEnding = ".class";
        CoverageDataStore.getInstance().addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), baseDir, fileEnding);
//        debugPrintChildren(CoverageDataStore.getInstance().getRoot(), 1);
        inst.addTransformer(new ClassTransformer());
    }

    // TODO: Remove debug
//    private static void debugPrintChildren(ExecutionDataNode<ExecutionData> pNode, int indent) {
//        if (pNode.isRoot()) {
//            ExecutionData rootData = pNode.getData();
//            String root = String.format("root %s %s %s %s", rootData.getMethodCount(), rootData.getTotal(), rootData.getCovered(), rootData.getMissed());
//            System.out.println(root);
//        }
//
//        Map<String, ExecutionDataNode<ExecutionData>> map = pNode.getChildren();
//        String strip = "";
//        for (int i = 0; i < indent; i++) {
//            strip = strip.concat("- ");
//        }
//        for (Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : map.entrySet()) {
//            ExecutionData data = entry.getValue().getData();
//            String str = String.format("%s%s %s %s %s %s", strip,
//                    entry.getKey(), data.getMethodCount(), data.getTotal(), data.getCovered(), data.getMissed());
//            System.out.println(str);
//            debugPrintChildren(entry.getValue(), indent + 1);
//        }
//    }
}
