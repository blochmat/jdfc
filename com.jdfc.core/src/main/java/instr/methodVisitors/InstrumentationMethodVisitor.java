package instr.methodVisitors;

import data.MethodData;
import data.ProgramVariable;
import data.singleton.CoverageDataStore;
import graphs.cfg.LocalVariable;
import instr.classVisitors.InstrumentationClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.util.UUID;

import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ISTORE;

public class InstrumentationMethodVisitor extends JDFCMethodVisitor {

    private final Logger logger = LoggerFactory.getLogger(InstrumentationClassVisitor.class);

    public InstrumentationMethodVisitor(InstrumentationClassVisitor pClassVisitor,
                                        MethodVisitor pMethodVisitor,
                                        MethodNode pMethodNode,
                                        String internalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, internalMethodName);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        insertLocalVariableEntryCreation(opcode, var);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        insertLocalVariableEntryCreation(ISTORE, var);
    }



    private void insertLocalVariableEntryCreation(final int opcode,
                                                  final int localVarIdx) {
//        mv.visitLdcInsn(classVisitor.classNode.name);
//        mv.visitLdcInsn(internalMethodName);
//        mv.visitLdcInsn(localVarIdx);
//        mv.visitLdcInsn(currentInstructionIndex);
//        mv.visitLdcInsn(currentLineNumber);
//        mv.visitLdcInsn(opcode);
        if (!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
            logger.debug("insertLocalVariableEntryCreation");
            UUID cId = classVisitor.classExecutionData.getId();
            UUID mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(currentLineNumber);
            MethodData mData = classVisitor.classExecutionData.getMethods().get(mId);
            LocalVariable localVariable = mData.getLocalVariableTable().get(localVarIdx);
            if(localVariable == null) {
                JDFCUtils.logThis("59: localVariable NULL", "insertLocalVariableEntryCreation");
                JDFCUtils.logThis("localVarIdx " + localVarIdx, "insertLocalVariableEntryCreation");
                JDFCUtils.logThis(classVisitor.classExecutionData.getName(), "insertLocalVariableEntryCreation");
                JDFCUtils.logThis(mData.getName(), "insertLocalVariableEntryCreation");
                JDFCUtils.logThis(JDFCUtils.prettyPrintMap(mData.getLocalVariableTable()), "insertLocalVariableEntryCreation");
            } else {
                ProgramVariable localPVar = new ProgramVariable(null, localVariable.getName(),
                        localVariable.getDescriptor(), currentInstructionIndex, currentLineNumber, this.isDefinition(opcode), false);
                UUID pId = mData.findVarId(localPVar);
                if (pId == null) {
                    if(logger.isDebugEnabled()) {
                        JDFCUtils.logThis("pId NULL", "insertLocalVariableEntryCreation");
                        JDFCUtils.logThis(localPVar.toString(), "insertLocalVariableEntryCreation");
                        JDFCUtils.logThis(classVisitor.classExecutionData.getName(), "insertLocalVariableEntryCreation");
                        JDFCUtils.logThis(mData.getName(), "insertLocalVariableEntryCreation");
                        JDFCUtils.logThis(JDFCUtils.prettyPrintMap(mData.getPVarToUUIDMap()), "insertLocalVariableEntryCreation");
                    }
                } else {
                    mv.visitLdcInsn(cId.toString());
                    mv.visitLdcInsn(mId.toString());
                    mv.visitLdcInsn(pId.toString());
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(CoverageDataStore.class),
                            "invokeCoverageTracker",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                            false);
                }
            }
        }
    }
}
