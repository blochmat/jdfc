package graphs.cfg.visitors.classVisitors;

import data.ClassExecutionData;
import graphs.cfg.visitors.methodVisitors.CFGAnalyzerAdapter;
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

    private String owner;

    public CFGNodeClassVisitor(final ClassNode pClassNode,
                               final ClassExecutionData pClassExecutionData) {
        super(Opcodes.ASM5, pClassNode, pClassExecutionData);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess, // 0, 1, 2
                                     final String pName, // e.g. max
                                     final String pDescriptor, // e.g. (II)I
                                     final String pSignature,  // more detailed desc
                                     final String[] pExceptions) // [ExcA, ExcB,.. ]
    {
        final MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        } else {
            mv = null;
        }

        if(classNode.access != Opcodes.ACC_INTERFACE && !JDFCUtils.isNestedClass(classNode.name)) {
            final MethodNode methodNode = this.getMethodNode(pName, pDescriptor);
            final String internalMethodName = asmHelper.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);

            // TODO
            if (methodNode != null
                    && isInstrumentationRequired(methodNode)
                    && !internalMethodName.contains("<clinit>")) {
                CFGAnalyzerAdapter aa = new CFGAnalyzerAdapter(Opcodes.ASM5, owner, pAccess, pName, pDescriptor, mv);

                return new CFGNodeMethodVisitor(this, mv, methodNode, internalMethodName, aa);
            }
        }

        return mv;
    }
}

