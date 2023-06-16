package instr.methodVisitors;

import data.ProgramVariable;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Stack;

import static org.objectweb.asm.Opcodes.*;

public abstract class JDFCMethodVisitor extends MethodVisitor {

    public final JDFCClassVisitor classVisitor;
    public final MethodNode methodNode;
    public String internalMethodName;
    public AbstractInsnNode currentNode = null;
    public int currentInstructionIndex = -1;
    public int currentLineNumber = -1;
    public int firstLine = -1;

    public Stack<ProgramVariable> operandStack;

    public JDFCMethodVisitor(final int pApi,
                             final JDFCClassVisitor pClassVisitor,
                             final MethodVisitor pMethodVisitor,
                             final MethodNode pMethodNode,
                             final String pInternalMethodName) {
        super(pApi, pMethodVisitor);
        this.classVisitor = pClassVisitor;
        this.methodNode = pMethodNode;
        this.internalMethodName = pInternalMethodName;
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (firstLine == -1) {
            if (!methodNode.name.contains("<init>")) {
                firstLine += line;
            } else {
                firstLine = line;
            }

        }
        currentLineNumber = line;
        super.visitLineNumber(line, start);
    }

//    @Override
//    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
//        updateCurrentNode();
//        super.visitFrame(type, numLocal, local, numStack, stack);
//    }

    @Override
    public void visitInsn(int opcode) {
        updateCurrentNode();
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        updateCurrentNode();
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        updateCurrentNode();
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        updateCurrentNode();
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        updateCurrentNode();
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        updateCurrentNode();
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        updateCurrentNode();
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        updateCurrentNode();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        updateCurrentNode();
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        updateCurrentNode();
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        updateCurrentNode();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        updateCurrentNode();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        updateCurrentNode();
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 9, maxLocals);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

    protected void updateCurrentNode() {
        if (currentNode == null) {
            currentNode = methodNode.instructions.getFirst();
        } else {
            if (currentNode.getNext() != null) {
                currentNode = currentNode.getNext();
            }
        }
        currentInstructionIndex = methodNode.instructions.indexOf(currentNode);
    }

    protected boolean isDefinition(final int pOpcode) {
        switch (pOpcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                return true;
            default:
                return false;
        }
    }


}
