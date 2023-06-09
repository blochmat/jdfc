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
        // TODO
        logger.debug("insertLocalVariableEntryCreation");
        if(!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
            MethodData mData = classVisitor.classExecutionData.getMethodByInternalName(internalMethodName);
            LocalVariable localVariable = mData.getLocalVariableTable().get(localVarIdx);
            if(localVariable != null) {
                UUID pId = UUID.randomUUID();
                ProgramVariable localPVar = new ProgramVariable(null, localVariable.getName(),
                        localVariable.getDescriptor(), currentInstructionIndex, currentLineNumber,
                        this.isDefinition(opcode), false);
                CoverageDataStore.getInstance().getUuidProgramVariableMap().put(pId, localPVar);
                UUID cId = CoverageDataStore.getInstance().getClassExecutionDataBiMap().inverse()
                        .get(classVisitor.classExecutionData);

                if (cId == null) {
                    logger.debug("IMPORTANT");
                    logger.debug("DATA");
                    logger.debug(classVisitor.classExecutionData.toString());
                    logger.debug("MAP");
                    logger.debug(JDFCUtils.prettyPrintMap(CoverageDataStore.getInstance().getClassExecutionDataBiMap()));
                } else {
                    mv.visitLdcInsn(pId.toString());
                    mv.visitLdcInsn(cId.toString());
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(CoverageDataStore.class),
                            "invokeCoverageTracker",
                            "(Ljava/lang/String;Ljava/lang/String;)V",
                            false);
                }

            }
        }
    }
}
