package com.jdfc.core.analysis.ifg;

import com.jdfc.core.analysis.JDFCClassVisitor;
import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.data.LocalVariableTable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

class CFGCreatorClassVisitor extends JDFCClassVisitor {

    private final Map<String, CFG> methodCFGs;

    public CFGCreatorClassVisitor(final ClassNode pClassNode,
                                  final ClassExecutionData pClassExecutionData,
                                  final Map<String, CFG> pMethodCFGs,
                                  final Map<String, LocalVariableTable> pLocalVariableTables) {
        super(Opcodes.ASM6, pClassNode, pClassExecutionData, pLocalVariableTables);
        methodCFGs = pMethodCFGs;
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        final MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        } else {
            mv = null;
        }
        final String internalMethodName = CFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        final LocalVariableTable localVariableTable = localVariableTables.get(internalMethodName);
        final Type[] parameterTypes = Type.getArgumentTypes(pDescriptor);
        final MethodNode methodNode = getMethodNode(pName);

        if (methodNode != null && isInstrumentationRequired(pName)) {
            return new CFGCreatorMethodVisitor(this, mv, pName, methodNode,
                    internalMethodName, methodCFGs, localVariableTable, parameterTypes);
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

        addInterProceduralEdges(methodCFGs);
        CoverageDataStore.getInstance().finishClassExecutionDataSetup(classExecutionData, methodCFGs);
    }

    private void addInterProceduralEdges(Map<String, CFG> pMethodCFGs) {
        for(Map.Entry<String, CFG> methodEntry : pMethodCFGs.entrySet()){
            for(Map.Entry<Integer, CFGNode> cfgNodeEntry : methodEntry.getValue().getNodes().entrySet()){
                if(cfgNodeEntry.getValue() instanceof IFGNode) {
                    IFGNode self = (IFGNode) cfgNodeEntry.getValue();
                    if (isInstrumentationRequired(self.getMethodNameDesc())) {
                        CFG other = pMethodCFGs.get(self.getMethodNameDesc());
                        self.setCallNode(other.getNodes().firstEntry().getValue());
                        self.setReturnNode(other.getNodes().lastEntry().getValue());
                    }
                }
            }
        }
    }
}

