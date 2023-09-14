package graphs.cfg.visitors.classVisitors;

import data.ClassData;
import graphs.cfg.visitors.methodVisitors.CFGAnalyzerAdapter;
import graphs.cfg.visitors.methodVisitors.CFGNodeMethodVisitor;
import instr.classVisitors.JDFCClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import utils.ASMHelper;

@Slf4j
public class CFGNodeClassVisitor extends JDFCClassVisitor {

    private final ASMHelper asmHelper = new ASMHelper();

    public String className;

    public CFGNodeClassVisitor(final ClassNode pClassNode,
                               final ClassData pClassData) {
        super(Opcodes.ASM5, pClassNode, pClassData);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int access, // 0, 1, 2
                                     final String name, // e.g. max
                                     final String descriptor, // e.g. (II)I
                                     final String signature,  // more detailed desc
                                     final String[] exceptions) // [ExcA, ExcB,.. ]
    {
        final MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        } else {
            mv = null;
        }

        final MethodNode methodNode = this.getMethodNode(name, descriptor);
        final String internalMethodName = asmHelper.computeInternalMethodName(name, descriptor, signature, exceptions);

        if (methodNode != null && isInstrumentationRequired(methodNode, internalMethodName)) {
            Type[] argTypes = Type.getArgumentTypes(descriptor);
            boolean isStatic = ((access & Opcodes.ACC_STATIC) != 0);
            CFGAnalyzerAdapter aa = new CFGAnalyzerAdapter(Opcodes.ASM5, className, access, name, descriptor, null);
            return new CFGNodeMethodVisitor(this, mv, methodNode, internalMethodName, aa, argTypes.length, isStatic);
        }

        return mv;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}

