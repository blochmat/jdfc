package instr.methodVisitors;

import data.MethodData;
import data.ProgramVariable;
import data.singleton.CoverageDataStore;
import graphs.cfg.LocalVariable;
import instr.classVisitors.InstrumentationClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ISTORE;
@Slf4j
public class InstrumentationMethodVisitor extends JDFCMethodVisitor {

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
        if (!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
            UUID cId = classVisitor.classExecutionData.getId();
            UUID mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(currentLineNumber);
            MethodData mData = classVisitor.classExecutionData.getMethods().get(mId);
            LocalVariable localVariable = mData.getLocalVariableTable().get(localVarIdx);
            if(localVariable == null) {
                if(log.isDebugEnabled()) {
                    File file = JDFCUtils.createFileInDebugDir("insertLocalVariableEntryCreation.txt", false);
                    try (FileWriter writer = new FileWriter(file, true)) {
                        writer.write("Error: LocalVariable is null.\n");
                        writer.write(String.format("  Class: %s\n", classVisitor.classExecutionData.getName()));
                        writer.write(String.format("  Method: %s\n", mData.buildInternalMethodName()));
                        writer.write(String.format("  localVarIdx: %d\n", localVarIdx));
                        writer.write("==============================\n");
                        writer.write("Local Variable Table:\n");
                        writer.write(JDFCUtils.prettyPrintMap(mData.getLocalVariableTable()));
                        writer.write("==============================\n");
                        writer.write("\n");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            } else {
                ProgramVariable localPVar = new ProgramVariable(null, localVariable.getName(),
                        localVariable.getDescriptor(), currentInstructionIndex, currentLineNumber, this.isDefinition(opcode), false);
                UUID pId = mData.findVarId(localPVar);
                if (pId == null) {
                    if(log.isDebugEnabled()) {
                        File file = JDFCUtils.createFileInDebugDir("insertLocalVariableEntryCreation.txt", false);
                        try (FileWriter writer = new FileWriter(file, true)) {
                            writer.write("Error: ProgramVariableId is null.\n");
                            writer.write(String.format("  Class: %s\n", classVisitor.classExecutionData.getName()));
                            writer.write(String.format("  Method: %s\n", mData.buildInternalMethodName()));
                            writer.write(String.format("  ProgramVariable: %s\n", localPVar));
                            writer.write("==============================\n");
                            writer.write("Program Variables:\n");
                            writer.write(JDFCUtils.prettyPrintMap(mData.getPVarToUUIDMap()));
                            writer.write("==============================\n");
                            writer.write("\n");
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
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
