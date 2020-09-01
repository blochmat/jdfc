package com.jdfc.agent.rt.internal;

import com.jdfc.commons.data.Node;
import com.jdfc.commons.data.NodeData;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.internal.data.ClassNodeData;
import com.jdfc.core.analysis.internal.data.PackageNodeData;

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
        PackageNodeData packageNodeData = new PackageNodeData();
        Node<NodeData> root = new Node<>(packageNodeData);
        List<String> classList = new ArrayList<>();
        Path baseDir = Paths.get(options).toAbsolutePath();
        CoverageDataStore.INSTANCE.setRoot(root);
        CoverageDataStore.INSTANCE.setClassList(classList);
        File f = new File(options);
        addNodes(f, root, baseDir);
        printTree(root);
        inst.addTransformer(new ClassTransformer());
    }

    private static void addNodes(File pFile, Node<NodeData> pNode, Path pBaseDir) {
        File[] fileList = Objects.requireNonNull(pFile.listFiles());

        for (File f : fileList){
            if(f.isDirectory()) {
                PackageNodeData pkgData = new PackageNodeData();
                Node<NodeData> newPkgNode = new Node<>(pkgData);
                pNode.addChild(f.getName(), newPkgNode);
                addNodes(f, newPkgNode, pBaseDir);
            } else if (f.isFile() && f.getName().endsWith(".class")) {
                String relativePath = pBaseDir.relativize(f.toPath()).toString();
                String relativePathWithoutType = relativePath.split("\\.")[0];
                // Add className to classList of storage. Thereby we determine, if class needs to be instrumented
                CoverageDataStore.INSTANCE.getClassList().add(relativePathWithoutType);

                String nameWithoutType = f.getName().split("\\.")[0];
                ClassNodeData classNodeData = new ClassNodeData();
                pNode.addChild(nameWithoutType, classNodeData);
            }
        }
    }

    private static void printTree(Node<NodeData> node) {
        if (!(node.getChildren().size() == 0)) {
            PrettyPrintMap<String, Node<NodeData>> prettyPrintMap = new PrettyPrintMap<>(node.getChildren());
            for(Map.Entry<String, Node<NodeData>> childNode : node.getChildren().entrySet()) {
                printTree(childNode.getValue());
            }
        }
    }
}
