package icfg.visitors.classVisitors;

import data.ClassExecutionData;
import data.CoverageDataStore;
import icfg.ICFG;
import icfg.ICFGCreator;
import icfg.data.LocalVariable;
import icfg.data.ProgramVariable;
import icfg.visitors.methodVisitors.ICFGNodeMethodVisitor;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class ICFGNodeClassVisitor extends JDFCClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(ICFGNodeClassVisitor.class);
    private final Map<String, ICFG> methodCFGs;

    public ICFGNodeClassVisitor(final ClassNode pClassNode,
                                final ClassExecutionData pClassExecutionData,
                                final Map<String, ICFG> pMethodCFGs,
                                final Set<ProgramVariable> fields,
                                final Map<String, Map<Integer, LocalVariable>> pLocalVariableTables) {
        super(Opcodes.ASM5, pClassNode, pClassExecutionData, fields, pLocalVariableTables);
        logger.debug(String.format("Visiting %s", pClassNode.name));
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
            // TODO: Do something with fields here
            if (methodNode != null && isInstrumentationRequired(pName)) {
                return new ICFGNodeMethodVisitor(this, mv, methodNode,
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

//        propagateInterProceduralInformation(methodCFGs);
        CoverageDataStore.getInstance().finishClassExecutionDataSetup(classExecutionData, methodCFGs);
    }

//    private void propagateInterProceduralInformation(Map<String, ICFG> pMethodCFGs) {
//        boolean propagateChange = true;
//        while (propagateChange) {
//            propagateChange = false;
//            for (Map.Entry<String, ICFG> methodEntry : pMethodCFGs.entrySet()) {
//                for (Map.Entry<Integer, ICFGNode> cfgNodeEntry : methodEntry.getValue().getNodes().entrySet()) {
//                    if (cfgNodeEntry.getValue() instanceof ToBeDeleted) {
//                        ToBeDeleted toBeDeleted = (ToBeDeleted) cfgNodeEntry.getValue();
//                        if (toBeDeleted.getMethodNameDesc() != null && isInstrumentationRequired(toBeDeleted.getMethodNameDesc())) {
//                            if (toBeDeleted.getRelatedCFG() == null && toBeDeleted.getMethodOwner().equals(classExecutionData.getRelativePath())) {
//                                ICFG otherICFG = pMethodCFGs.get(toBeDeleted.getMethodNameDesc());
//                                toBeDeleted.setupMethodRelation(otherICFG);
//                                if (otherICFG.isImpure() && !methodEntry.getValue().isImpure()) {
//                                    methodEntry.getValue().setImpure();
//                                    propagateChange = true;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
}

