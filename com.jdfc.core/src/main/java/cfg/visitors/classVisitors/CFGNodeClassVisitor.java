package cfg.visitors.classVisitors;

import cfg.CFG;
import cfg.CFGCreator;
import cfg.data.LocalVariable;
import cfg.visitors.methodVisitors.CFGNodeMethodVisitor;
import data.ClassExecutionData;
import data.CoverageDataStore;
import data.MethodData;
import data.ProgramVariable;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.util.Map;
import java.util.Set;

public class CFGNodeClassVisitor extends JDFCClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(CFGNodeClassVisitor.class);
    private final Map<String, CFG> methodCFGs;

    public CFGNodeClassVisitor(final ClassNode pClassNode,
                               final ClassExecutionData pClassExecutionData,
                               final Map<String, CFG> pMethodCFGs,
                               final Set<ProgramVariable> fields,
                               final Map<String, Map<Integer, LocalVariable>> pLocalVariableTables) {
        super(Opcodes.ASM5, pClassNode, pClassExecutionData, fields, pLocalVariableTables);
        methodCFGs = pMethodCFGs;
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess, // 0, 1, 2
                                     final String pName, // e.g. max
                                     final String pDescriptor, // e.g. (II)I
                                     final String pSignature,  // idk
                                     final String[] pExceptions) // idk
    {

        final MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        } else {
            mv = null;
        }

        if(classNode.access != Opcodes.ACC_INTERFACE && !JDFCUtils.isInnerClass(classNode.name)) {
            final MethodNode methodNode = getMethodNode(pName, pDescriptor);
            final String internalMethodName = CFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
            final Map<Integer, LocalVariable> localVariableTable = localVariableTables.get(internalMethodName);

            // New Code
            MethodData mData = this.classExecutionData.getMethodByName(pName);

            if (methodNode != null && isInstrumentationRequired(pName)) {
                return new CFGNodeMethodVisitor(this, mv, methodNode,
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

//        propagateInterProceduralInformation(methodCFGs)
        if(classNode.access != Opcodes.ACC_INTERFACE && !JDFCUtils.isInnerClass(classNode.name)) {
            CoverageDataStore.getInstance().finishClassExecutionDataSetup(classExecutionData, methodCFGs);
        }
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

