package icfg;

import data.ClassExecutionData;
import data.CoverageDataStore;
import icfg.data.LocalVariable;
import instr.JDFCClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

class CFGCreatorClassVisitor extends JDFCClassVisitor {

    private final Map<String, CFG> methodCFGs;

    public CFGCreatorClassVisitor(final ClassNode pClassNode,
                                  final ClassExecutionData pClassExecutionData,
                                  final Map<String, CFG> pMethodCFGs,
                                  final Map<String, Map<Integer, LocalVariable>> pLocalVariableTables) {
        super(Opcodes.ASM5, pClassNode, pClassExecutionData, pLocalVariableTables);
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

        if(classNode.access != Opcodes.ACC_INTERFACE) {
            final String internalMethodName = CFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
            final Map<Integer, LocalVariable> localVariableTable = localVariableTables.get(internalMethodName);
            final MethodNode methodNode = getMethodNode(pName, pDescriptor);
            if (methodNode != null && isInstrumentationRequired(pName)) {
                return new CFGCreatorMethodVisitor(this, mv, methodNode,
                        internalMethodName, methodCFGs, localVariableTable);
            }
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

        propagateInterProceduralInformation(methodCFGs);
        CoverageDataStore.getInstance().finishClassExecutionDataSetup(classExecutionData, methodCFGs);
    }

    private void propagateInterProceduralInformation(Map<String, CFG> pMethodCFGs) {
        boolean propagateChange = true;
        while (propagateChange) {
            propagateChange = false;
            for (Map.Entry<String, CFG> methodEntry : pMethodCFGs.entrySet()) {
                for (Map.Entry<Integer, CFGNode> cfgNodeEntry : methodEntry.getValue().getNodes().entrySet()) {
                    if (cfgNodeEntry.getValue() instanceof IFGNode) {
                        IFGNode ifgNode = (IFGNode) cfgNodeEntry.getValue();
                        if (ifgNode.getMethodNameDesc() != null && isInstrumentationRequired(ifgNode.getMethodNameDesc())) {
                            if (ifgNode.getRelatedCFG() == null && ifgNode.getMethodOwner().equals(classExecutionData.getRelativePath())) {
                                CFG otherCFG = pMethodCFGs.get(ifgNode.getMethodNameDesc());
                                ifgNode.setupMethodRelation(otherCFG);
                                if (otherCFG.isImpure() && !methodEntry.getValue().isImpure()) {
                                    methodEntry.getValue().setImpure();
                                    propagateChange = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

