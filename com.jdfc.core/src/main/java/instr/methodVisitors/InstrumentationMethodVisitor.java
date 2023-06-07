package instr.methodVisitors;

import data.singleton.CoverageDataStore;
import instr.classVisitors.InstrumentationClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ISTORE;

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



    private void insertLocalVariableEntryCreation(final int pOpcode,
                                                  final int pVar) {
        mv.visitLdcInsn(classVisitor.classNode.name);
        mv.visitLdcInsn(internalMethodName);
        mv.visitLdcInsn(pVar);
        mv.visitLdcInsn(currentInstructionIndex);
        mv.visitLdcInsn(currentLineNumber);
        mv.visitLdcInsn(pOpcode);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(CoverageDataStore.class),
                "invokeCoverageTracker",
                "(Ljava/lang/String;" +
                        "Ljava/lang/String;" +
                        "IIII)V",
                false);
    }
}
