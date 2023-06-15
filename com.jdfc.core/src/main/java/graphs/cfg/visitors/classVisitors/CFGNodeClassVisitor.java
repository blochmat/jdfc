package graphs.cfg.visitors.classVisitors;

import data.ClassExecutionData;
import graphs.cfg.visitors.methodVisitors.CFGNodeMethodVisitor;
import instr.classVisitors.JDFCClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import utils.ASMHelper;
import utils.JDFCUtils;

@Slf4j
public class CFGNodeClassVisitor extends JDFCClassVisitor {

    private final ASMHelper asmHelper = new ASMHelper();

    String owner;

    public CFGNodeClassVisitor(final ClassNode pClassNode,
                               final ClassExecutionData pClassExecutionData) {
        super(Opcodes.ASM5, pClassNode, pClassExecutionData);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.owner = name;
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

        if(classNode.access != Opcodes.ACC_INTERFACE && !JDFCUtils.isNestedClass(classNode.name)) {
            final MethodNode methodNode = this.getMethodNode(name, descriptor);
            final String internalMethodName = asmHelper.computeInternalMethodName(name, descriptor, signature, exceptions);

            // TODO
            if (methodNode != null
                    && isInstrumentationRequired(methodNode)
                    && !internalMethodName.contains("<init>")
                    && !internalMethodName.contains("<clinit>")) {

                return new CFGNodeMethodVisitor(owner, access, name, descriptor, this, mv, methodNode, internalMethodName);
            }
        }

        return mv;
    }
}

