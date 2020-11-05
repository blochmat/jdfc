package com.jdfc.core.report;

import com.jdfc.core.analysis.JDFCClassVisitor;
import com.jdfc.core.analysis.JDFCMethodVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM6;

public class ReportMethodVisitor extends JDFCMethodVisitor {

    private final Map<Integer, Integer> opcodes;
    private final int lineNumber;

    Map<Integer, Integer> getOpcodes() {
        return opcodes;
    }

    public ReportMethodVisitor(final JDFCClassVisitor pClassVisitor,
                               final MethodVisitor pMethodVisitor,
                               final MethodNode pMethodNode,
                               final String pInternalMethodName,
                               final int pLineNumber) {
        super(ASM6, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
        opcodes = new HashMap<>();
        lineNumber = pLineNumber;
    }

//    @Override
//    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
//        super.visitFrame(type, numLocal, local, numStack, stack);
//    }
//
//    @Override
//    public void visitInsn(int opcode) {
//        super.visitInsn(opcode);
//        if(currentLineNumber == lineNumber) {
//            opcodes.put(currentInstructionIndex, opcode);
//        }
//    }
//
//    @Override
//    public void visitIntInsn(int opcode, int operand) {
//        super.visitIntInsn(opcode, operand);
//        if(currentLineNumber == lineNumber) {
//            opcodes.put(currentInstructionIndex, opcode);
//        }
//    }
//
//    @Override
//    public void visitVarInsn(int opcode, int var) {
//        super.visitVarInsn(opcode, var);
//        if(currentLineNumber == lineNumber) {
//            opcodes.put(currentInstructionIndex, opcode);
//        }
//    }
//
//    @Override
//    public void visitTypeInsn(int opcode, String type) {
//        super.visitTypeInsn(opcode, type);
//        if(currentLineNumber == lineNumber) {
//            opcodes.put(currentInstructionIndex, opcode);
//        }
//    }
//
//    @Override
//    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
//        super.visitFieldInsn(opcode, owner, name, descriptor);
//        if(currentLineNumber == lineNumber) {
//            opcodes.put(currentInstructionIndex, opcode);
//        }
//    }
//
//    @Override
//    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
//        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
//        if(currentLineNumber == lineNumber) {
//            opcodes.put(currentInstructionIndex, opcode);
//        }
//    }
//
//    @Override
//    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
//        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
//    }
//
//    @Override
//    public void visitJumpInsn(int opcode, Label label) {
//        super.visitJumpInsn(opcode, label);
//        if(currentLineNumber == lineNumber) {
//            opcodes.put(currentInstructionIndex, opcode);
//        }
//    }
//
//    @Override
//    public void visitLdcInsn(Object value) {
//        super.visitLdcInsn(value);
//    }
//
//    @Override
//    public void visitIincInsn(int var, int increment) {
//        super.visitIincInsn(var, increment);
//
//    }
//
//    @Override
//    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
//        super.visitTableSwitchInsn(min, max, dflt, labels);
//
//    }
//
//    @Override
//    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//        super.visitLookupSwitchInsn(dflt, keys, labels);
//
//    }
//
//    @Override
//    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
//        super.visitMultiANewArrayInsn(descriptor, numDimensions);
//    }
//
//    @Override
//    public void visitEnd() {
//        super.visitEnd();
//    }
}
