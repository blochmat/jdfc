package instr.methodVisitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class JDFCAdviceAdapter extends AdviceAdapter {

    Label startTryLabel = new Label();
    Label startCatchLabel = new Label();
    Label endTryLabel = new Label();

    /**
     * Constructs a new {@link AdviceAdapter}.
     *
     * @param api           the ASM API version implemented by this visitor. Must be one of {@link
     *                      Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7}.
     * @param methodVisitor the method visitor to which this adapter delegates calls.
     * @param access        the method's access flags (see {@link Opcodes}).
     * @param name          the method's name.
     * @param descriptor    the method's descriptor (see {@link Type Type}).
     */
    public JDFCAdviceAdapter(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(api, methodVisitor, access, name, descriptor);
    }

    @Override
    protected void onMethodEnter() {
        // Mark the start of the try block
        mv.visitLabel(startTryLabel);
//        mv.visitTryCatchBlock(startTryLabel, startCatchLabel, startCatchLabel, "java/lang/Exception");
    }

    @Override
    protected void onMethodExit(int opcode) {
        // If the method normally completes or completes abruptly because of an exception, it will go to this block
        if (opcode == ATHROW) {
            // Catch part:
            // Catch any Exception (catch (Exception e))
            mv.visitLabel(startCatchLabel);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Exception"});
            mv.visitVarInsn(ASTORE, 1); // e = exception
            // Add any custom processing here, e.g., logging

            // Exit from catch block
            mv.visitLabel(endTryLabel);
        }
    }

//    @Override
//    public void visitMaxs(int maxStack, int maxLocals) {
//        // Handles the exception
//        super.visitMaxs(maxStack, maxLocals);
//    }
}
