package instr.classVisitors;

import instr.methodVisitors.AddTryCatchAdviceAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class AddTryCatchClassVisitor extends ClassVisitor {

    public AddTryCatchClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new AddTryCatchAdviceAdapter(api, mv, access, name, descriptor);
    }
}
