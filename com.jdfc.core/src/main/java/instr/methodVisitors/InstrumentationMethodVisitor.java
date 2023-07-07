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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ISTORE;
@Slf4j
public class InstrumentationMethodVisitor extends JDFCMethodVisitor {

    private static final String COVERAGE_DATA_STORE = Type.getInternalName(CoverageDataStore.class);

    private static final String TRACK_VAR = "trackVar";
    private static final String TRACK_VAR_DESC = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

    private static final String TRACK_NEW_OBJECT = "trackNewObject";
    private static final String TRACK_NEW_OBJECT_DESC = "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V";

    private static final String TRACK_MODIFIED_OBJECT = "trackModifiedObject";
    private static final String TRACK_MODIFIED_OBJECT_DESC = "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V";

    private final Map<Integer, Boolean> trackObject = new HashMap<>();

    public InstrumentationMethodVisitor(InstrumentationClassVisitor pClassVisitor,
                                        MethodVisitor pMethodVisitor,
                                        MethodNode pMethodNode,
                                        String internalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, internalMethodName);
    }

//    @Override
//    public void visitTypeInsn(int opcode, String type) {
//        super.visitTypeInsn(opcode, type);
//        if(!internalMethodName.contains("<clinit>") && opcode == Opcodes.NEW) {
//
//            UUID cId = classVisitor.classExecutionData.getId();
//            UUID mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(currentLineNumber);
//
//            mv.visitInsn(Opcodes.DUP);
//            mv.visitLdcInsn(cId.toString());
//            mv.visitLdcInsn(mId.toString());
//            mv.visitMethodInsn(
//                    Opcodes.INVOKESTATIC,
//                    COVERAGE_DATA_STORE,
//                    TRACK_NEW_OBJECT,
//                    TRACK_NEW_OBJECT_DESC,
//                    false);
//        }
//    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        insertFieldTracking(opcode, owner, name, descriptor);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        insertLocalVarTracking(opcode, var);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        insertLocalVarTracking(ISTORE, var);
    }

    public void insertFieldTracking(int opcode, String className, String name, String descriptor) {
        if (!internalMethodName.contains("<clinit>")) {
            UUID cId = classVisitor.classExecutionData.getId();
            UUID mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(currentLineNumber);
            if(mId == null && internalMethodName.equals("<init>: ()V;")) {
                // Default constructor
                mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(Integer.MIN_VALUE);
            }

            if(mId != null) {
                MethodData mData = classVisitor.classExecutionData.getMethods().get(mId);
                ProgramVariable localPVar = new ProgramVariable(
                        null,
                        Integer.MIN_VALUE,
                        className,
                        mData.buildInternalMethodName(),
                        name,
                        descriptor,
                        currentInstructionIndex,
                        currentLineNumber,
                        this.isDefinition(opcode),
                        false,
                        true);
                UUID pId = mData.findVarId(localPVar);
                if (pId == null) {
                    if(log.isDebugEnabled()) {
                        File file = JDFCUtils.createFileInDebugDir("insertLocalVarTracking.txt", false);
                        try (FileWriter writer = new FileWriter(file, true)) {
                            writer.write("Error: ProgramVariableId is null.\n");
                            writer.write(String.format("  Class: %s\n", classVisitor.classExecutionData.getName()));
                            writer.write(String.format("  Method: %s\n", mData.buildInternalMethodName()));
                            writer.write(String.format("  ProgramVariable: %s\n", localPVar));
                            writer.write("==============================\n");
                            writer.write("Program Variables:\n");
                            writer.write(JDFCUtils.prettyPrintMap(mData.getProgramVariables()));
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
                            COVERAGE_DATA_STORE,
                            TRACK_VAR,
                            TRACK_VAR_DESC,
                            false);
                }
            } else {
                if(log.isDebugEnabled()) {
                    String error = String.format("%s::%s : mId == null\n%s%s",
                            classVisitor.classExecutionData.getRelativePath(),
                            internalMethodName,
                            JDFCUtils.prettyPrintMap(classVisitor.classExecutionData.getMethods()),
                            JDFCUtils.prettyPrintMap(classVisitor.classExecutionData.getLineToMethodIdMap())
                    );
                    JDFCUtils.logThis(error, "ERROR");
                }
            }
        }
    }

    private void insertLocalVarTracking(final int opcode,
                                        final int localVarIdx) {
        if (!internalMethodName.contains("<clinit>")) {
            UUID cId = classVisitor.classExecutionData.getId();
            UUID mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(currentLineNumber);
            if(mId == null && internalMethodName.equals("<init>: ()V;")) {
                // Default constructor
                mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(Integer.MIN_VALUE);
            }

            if(mId != null) {
                MethodData mData = classVisitor.classExecutionData.getMethods().get(mId);
                LocalVariable localVariable = mData.getLocalVariableTable().get(localVarIdx);
                if(localVariable == null) {
                    if(log.isDebugEnabled()) {
                        File file = JDFCUtils.createFileInDebugDir("insertLocalVarTracking.txt", false);
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
                    ProgramVariable localPVar = new ProgramVariable(
                            null,
                            localVarIdx,
                            mData.getClassName(),
                            mData.buildInternalMethodName(),
                            localVariable.getName(),
                            localVariable.getDescriptor(),
                            currentInstructionIndex,
                            currentLineNumber,
                            this.isDefinition(opcode),
                            false,
                            false);
                    UUID pId = mData.findVarId(localPVar);
                    if (pId == null) {
                        if(log.isDebugEnabled()) {
                            File file = JDFCUtils.createFileInDebugDir("insertLocalVarTracking.txt", false);
                            try (FileWriter writer = new FileWriter(file, true)) {
                                writer.write("Error: ProgramVariableId is null.\n");
                                writer.write(String.format("  Class: %s\n", classVisitor.classExecutionData.getName()));
                                writer.write(String.format("  Method: %s\n", mData.buildInternalMethodName()));
                                writer.write(String.format("  ProgramVariable: %s\n", localPVar));
                                writer.write("==============================\n");
                                writer.write("Program Variables:\n");
                                writer.write(JDFCUtils.prettyPrintMap(mData.getProgramVariables()));
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
                                COVERAGE_DATA_STORE,
                                TRACK_VAR,
                                TRACK_VAR_DESC,
                                false);
                    }
                }
            }
        }
    }
}
