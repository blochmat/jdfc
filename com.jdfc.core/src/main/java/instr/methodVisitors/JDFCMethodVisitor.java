package instr.methodVisitors;

import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public abstract class JDFCMethodVisitor extends MethodVisitor {

    public final JDFCClassVisitor classVisitor;
    public final MethodNode methodNode;
    public String internalMethodName;
    public AbstractInsnNode currentNode = null;
    public int currentInstructionIndex = -1;
    public int currentLineNumber = -1;
    public int firstLine = -1;

    public JDFCMethodVisitor(final int pApi,
                             final JDFCClassVisitor pClassVisitor,
                             final MethodVisitor pMethodVisitor,
                             final MethodNode pMethodNode,
                             final String pInternalMethodName) {
        super(pApi, pMethodVisitor);
        classVisitor = pClassVisitor;
        methodNode = pMethodNode;
        internalMethodName = pInternalMethodName;
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

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        updateCurrentNode();
        super.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitInsn(int opcode) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitTypeInsn(opcode, type);
    }


    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        updateCurrentNode();
        checkForF_NEW();
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

//    @Override
//    public void visitMaxs(int maxStack, int maxLocals) {
//        super.visitMaxs(maxStack + 9, maxLocals);
//    }

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

    public void checkForF_NEW() {
        if (currentNode.getOpcode() == F_NEW) {
            updateCurrentNode();
        }
    }

    protected boolean isDefinition(final int pOpcode) {
        switch (pOpcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
            case PUTFIELD:
            case PUTSTATIC:
                return true;
            default:
                return false;
        }
    }
}
