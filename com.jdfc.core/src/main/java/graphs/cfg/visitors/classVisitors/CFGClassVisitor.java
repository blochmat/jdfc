package graphs.cfg.visitors.classVisitors;

import data.ClassData;
import graphs.cfg.visitors.methodVisitors.CFGAnalyzerAdapter;
import graphs.cfg.visitors.methodVisitors.CFGMethodVisitor;
import instr.classVisitors.JDFCClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import utils.ASMHelper;

import static org.objectweb.asm.Opcodes.ASM5;

@Slf4j
public class CFGClassVisitor extends JDFCClassVisitor {

    private final ASMHelper asmHelper;

    public CFGClassVisitor(ClassNode classNode, ClassData classData) {
        super(ASM5, classNode, classData);
        this.asmHelper = new ASMHelper();
    }

    @Override
    public MethodVisitor visitMethod(final int access, // 0, 1, 2
                                     final String name, // e.g. max
                                     final String descriptor, // e.g. (II)I
                                     final String signature,  // more detailed desc for generics
                                     final String[] exceptions) // [ExcA, ExcB,.. ]
    {
        final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        final MethodNode methodNode = this.asmHelper.getMethodNode(this.classNode, name, descriptor, signature);
        final String internalMethodName = this.asmHelper.computeInternalMethodName(name, descriptor, signature, exceptions);
        final Type[] argTypes = Type.getArgumentTypes(descriptor);
        final boolean isStatic = this.asmHelper.isStatic(access);

        if (methodNode != null && this.asmHelper.isInstrumentationRequired(methodNode, internalMethodName)) {
            CFGAnalyzerAdapter aa = new CFGAnalyzerAdapter(ASM5,
                    this.classData.getClassMetaData().getClassFileRelNoType(), access, name, descriptor, null);
            return new CFGMethodVisitor(this, mv, methodNode, internalMethodName, aa, argTypes.length, isStatic);
        }

        return mv;
    }
}

