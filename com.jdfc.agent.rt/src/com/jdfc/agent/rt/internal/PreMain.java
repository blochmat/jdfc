package com.jdfc.agent.rt.internal;

import com.jdfc.commons.data.Node;
import com.jdfc.commons.data.NodeData;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.internal.data.ClassNodeData;
import com.jdfc.core.analysis.internal.data.PackageNodeData;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
                String nameWithoutType = relativePath.split("\\.")[0];

                // Add className to classList of storage. Thereby we determine, if class needs to be instrumented
                CoverageDataStore.INSTANCE.getClassList().add(nameWithoutType);
                ClassNodeData classNodeData = new ClassNodeData();
                pNode.addChild(f.getName(), classNodeData);
            }
        }
    }
}
