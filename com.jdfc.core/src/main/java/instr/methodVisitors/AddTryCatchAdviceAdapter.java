package instr.methodVisitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class AddTryCatchAdviceAdapter extends AdviceAdapter {

    private final Label startTryLabel = new Label();
    private final Label endTryLabel = new Label();
    private final Label startCatchLabel = new Label();

    public AddTryCatchAdviceAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
    }

    @Override
    protected void onMethodEnter() {
        mv.visitLabel(startTryLabel);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != Opcodes.ATHROW) {
            mv.visitLabel(endTryLabel);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitLabel(startCatchLabel);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitTryCatchBlock(startTryLabel, endTryLabel, startCatchLabel, "java/lang/Exception");
        super.visitMaxs(maxStack, maxLocals);
    }
}

