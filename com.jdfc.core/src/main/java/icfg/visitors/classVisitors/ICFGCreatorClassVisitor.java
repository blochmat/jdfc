package icfg.visitors.classVisitors;

import data.ClassExecutionData;
import data.CoverageDataStore;
import icfg.ICFG;
import icfg.ICFGCreator;
import icfg.ToBeDeleted;
import icfg.data.LocalVariable;
import icfg.nodes.ICFGNode;
import icfg.visitors.methodVisitors.ICFGCreatorMethodVisitor;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

public class ICFGCreatorClassVisitor extends JDFCClassVisitor {

    private final Map<String, ICFG> methodCFGs;

    public ICFGCreatorClassVisitor(final ClassNode pClassNode,
                                   final ClassExecutionData pClassExecutionData,
                                   final Map<String, ICFG> pMethodCFGs,
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
            final String internalMethodName = ICFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
            final Map<Integer, LocalVariable> localVariableTable = localVariableTables.get(internalMethodName);
            final MethodNode methodNode = getMethodNode(pName, pDescriptor);
            if (methodNode != null && isInstrumentationRequired(pName)) {
                return new ICFGCreatorMethodVisitor(this, mv, methodNode,
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

    private void propagateInterProceduralInformation(Map<String, ICFG> pMethodCFGs) {
        boolean propagateChange = true;
        while (propagateChange) {
            propagateChange = false;
            for (Map.Entry<String, ICFG> methodEntry : pMethodCFGs.entrySet()) {
                for (Map.Entry<Integer, ICFGNode> cfgNodeEntry : methodEntry.getValue().getNodes().entrySet()) {
                    if (cfgNodeEntry.getValue() instanceof ToBeDeleted) {
                        ToBeDeleted toBeDeleted = (ToBeDeleted) cfgNodeEntry.getValue();
                        if (toBeDeleted.getMethodNameDesc() != null && isInstrumentationRequired(toBeDeleted.getMethodNameDesc())) {
                            if (toBeDeleted.getRelatedCFG() == null && toBeDeleted.getMethodOwner().equals(classExecutionData.getRelativePath())) {
                                ICFG otherICFG = pMethodCFGs.get(toBeDeleted.getMethodNameDesc());
                                toBeDeleted.setupMethodRelation(otherICFG);
                                if (otherICFG.isImpure() && !methodEntry.getValue().isImpure()) {
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

