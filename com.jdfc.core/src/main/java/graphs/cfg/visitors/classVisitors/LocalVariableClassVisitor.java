package graphs.cfg.visitors.classVisitors;

import data.ClassData;
import graphs.cfg.visitors.methodVisitors.CFGLocalVariableMethodVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import utils.ASMHelper;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * A class visitor that extracts the information of the local variable table for each method of a
 * class.
 *
 * @see ClassVisitor
 */
@Slf4j
public class LocalVariableClassVisitor extends ClassVisitor {

    private final ClassNode classNode;
    private final ClassData classData;
    private final ASMHelper asmHelper;

    public LocalVariableClassVisitor(final ClassNode classNode, final ClassData classData) {
        super(ASM5);
        this.classNode = classNode;
        this.classData = classData;
        this.asmHelper = new ASMHelper();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(final int access,
                                     final String name,
                                     final String descriptor,
                                     final String signature,
                                     final String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        final MethodNode methodNode = this.asmHelper.getMethodNode(this.classNode, name, descriptor, signature);
        final String internalMethodName = this.asmHelper.computeInternalMethodName(name, descriptor, signature, exceptions);

        if (methodNode != null && this.asmHelper.isInstrumentationRequired(methodNode, internalMethodName)) {
            return new CFGLocalVariableMethodVisitor(this.classData.getMethodByInternalName(internalMethodName));
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        } else {
            super.visitEnd();
        }
    }
}
