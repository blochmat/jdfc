package com.jdfc.core.analysis.ifg;

import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.Map;

public class CFGCreatorClassVisitor extends ClassVisitor {

    private final ClassNode classNode;
    private final ClassExecutionData classExecutionData;
    private final Map<String, CFG> methodCFGs;
    private final Map<String, LocalVariableTable> localVariableTables;
    final String jacocoMethodName = "$jacoco";

    public CFGCreatorClassVisitor(final ClassNode pClassNode,
                                  final ClassExecutionData pClassExecutionData,
                                  final Map<String, CFG> pMethodCFGs,
                                  final Map<String, LocalVariableTable> pLocalVariableTables) {
        super(Opcodes.ASM6);
        classNode = pClassNode;
        methodCFGs = pMethodCFGs;
        localVariableTables = pLocalVariableTables;
        classExecutionData = pClassExecutionData;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//        System.out.println("visitMethod");
        MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(access, name, desc, signature, exceptions);
        } else {
            mv = null;
        }
        final String internalMethodName = CFGCreator.computeInternalMethodName(name, desc, signature, exceptions);
        final LocalVariableTable localVariableTable = localVariableTables.get(internalMethodName);
        final Type[] parameterTypes = Type.getArgumentTypes(desc);
        final MethodNode methodNode = getMethodNode(name);

        if (methodNode != null && !isJacocoInstrumentation(name)) {
            return new CFGCreatorMethodVisitor(
                    classNode.name, classExecutionData, mv, methodNode, name, internalMethodName, methodCFGs, localVariableTable, parameterTypes);
        }

        return mv;
    }

    @Override
    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        } else {
            super.visitEnd();
        }
        // Create interpocedural edges when we are sure all subgraphs are there
        addInterproceduralEdges(methodCFGs);
        CoverageDataStore.getInstance().finishClassExecutionDataSetup(classExecutionData, methodCFGs);
    }

    private MethodNode getMethodNode(String pName) {
//        System.out.println("getMethodNode");
        for (MethodNode node : classNode.methods) {
            if (node.name.equals(pName)) {
                return node;
            }
        }
        return null;
    }

    private static void addInterproceduralEdges(Map<String, CFG> pMethodCFGs) {
        for(Map.Entry<String, CFG> methodEntry : pMethodCFGs.entrySet()){
            for(Map.Entry<Integer, CFGNode> cfgNodeEntry : methodEntry.getValue().getNodes().entrySet()){
                if(cfgNodeEntry.getValue() instanceof IFGNode) {
                    IFGNode self = (IFGNode) cfgNodeEntry.getValue();
                    CFG other = pMethodCFGs.get(self.getMethodNameDesc());
                    self.setCallNode(other.getNodes().firstEntry().getValue());
                    self.setReturnNode(other.getNodes().lastEntry().getValue());
                }
            }
        }
    }

    private boolean isJacocoInstrumentation(String pString) {
        return pString.contains(jacocoMethodName);
    }
}

