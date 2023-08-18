package instr.methodVisitors;

import data.MethodData;
import data.ProgramVariable;
import data.singleton.CoverageDataStore;
import graphs.cfg.LocalVariable;
import instr.classVisitors.InstrumentationClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.objectweb.asm.Opcodes.*;

@Slf4j
public class InstrumentationMethodVisitor extends JDFCMethodVisitor {

    private static final String COVERAGE_DATA_STORE = Type.getInternalName(CoverageDataStore.class);

    private static final String TRACK_VAR = "trackVar";
    private static final String TRACK_VAR_DESC = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

    private static final String TRACK_NEW_OBJECT = "trackNewObject";
    private static final String TRACK_NEW_OBJECT_DESC = "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V";

    private static final String TRACK_MODIFIED_OBJECT = "trackModifiedObject";
    private static final String TRACK_MODIFIED_OBJECT_DESC = "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V";

    private final Set<Integer> trackObject = new HashSet<>();
//    private final CFGAnalyzerAdapter aa;

    public InstrumentationMethodVisitor(InstrumentationClassVisitor pClassVisitor,
                                        MethodVisitor pMethodVisitor,
                                        MethodNode pMethodNode,
                                        String internalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, internalMethodName);
//        this.aa = aa;
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        super.visitFrame(type, numLocal, local, numStack, stack);
//        aa.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
//        aa.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
//        aa.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
//        if(opcode == Opcodes.ALOAD && var == 0) {
//            mv.visitInsn(Opcodes.DUP);
//        }
//        aa.visitVarInsn(opcode, var);
        insertLocalVarTracking(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
//        aa.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
//        aa.visitFieldInsn(opcode, owner, name, descriptor);
        super.visitFieldInsn(opcode, owner, name, descriptor);
//        if(opcode == Opcodes.PUTFIELD || opcode == PUTSTATIC) {
//            insertModifiedObjectTracking();
//        }
        insertFieldTracking(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
//        if(opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
//            if (owner.equals("java/lang/Exception")) {
//                mv.visitInsn(Opcodes.DUP);
//                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
//            } else {
//                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
//                mv.visitInsn(Opcodes.DUP);
//            }
//            insertNewObjectTracking();
//        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
//        }
//        aa.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
//        aa.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
//        aa.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
//        aa.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
//        aa.visitIincInsn(var, increment);
        insertLocalVarTracking(ISTORE, var);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
//        aa.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
//        aa.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
//        aa.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
//        aa.visitEnd();
    }

    private void insertNewObjectTracking() {
        UUID cId = classVisitor.classExecutionData.getId();
        UUID mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(currentLineNumber);
        if(mId == null && internalMethodName.equals("<init>: ()V;")) {
            // Default constructor
            mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(Integer.MIN_VALUE);
        }

        if(mId != null) {
            mv.visitLdcInsn(cId.toString());
            mv.visitLdcInsn(mId.toString());
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    COVERAGE_DATA_STORE,
                    TRACK_NEW_OBJECT,
                    TRACK_NEW_OBJECT_DESC,
                    false);

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

    private void insertModifiedObjectTracking() {
        UUID cId = classVisitor.classExecutionData.getId();
        UUID mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(currentLineNumber);
        if(mId == null && internalMethodName.equals("<init>: ()V;")) {
            // Default constructor
            mId = classVisitor.classExecutionData.getLineToMethodIdMap().get(Integer.MIN_VALUE);
        }

        if(mId != null) {
            mv.visitLdcInsn(cId.toString());
            mv.visitLdcInsn(mId.toString());
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    COVERAGE_DATA_STORE,
                    TRACK_MODIFIED_OBJECT,
                    TRACK_MODIFIED_OBJECT_DESC,
                    false);

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
                        File file = JDFCUtils.createFileInDebugDir("ERROR_insertFieldTracking.txt", false);
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
                        File file = JDFCUtils.createFileInDebugDir("ERROR_insertLocalVarTracking.txt", false);
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
                            File file = JDFCUtils.createFileInDebugDir("ERROR_insertLocalVarTracking.txt", false);
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
