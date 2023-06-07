package graphs.cfg.visitors.classVisitors;

import graphs.cfg.visitors.methodVisitors.CFGNodeMethodVisitor;
import data.ClassExecutionData;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ASMHelper;
import utils.JDFCUtils;

public class CFGNodeClassVisitor extends JDFCClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(CFGNodeClassVisitor.class);

    private final ASMHelper asmHelper = new ASMHelper();

    public CFGNodeClassVisitor(final ClassNode pClassNode,
                               final ClassExecutionData pClassExecutionData) {
        super(Opcodes.ASM5, pClassNode, pClassExecutionData);
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess, // 0, 1, 2
                                     final String pName, // e.g. max
                                     final String pDescriptor, // e.g. (II)I
                                     final String pSignature,  // more detailed desc
                                     final String[] pExceptions) // [ExcA, ExcB,.. ]
    {
        logger.debug("visitMethod");
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
            if (methodNode != null && isInstrumentationRequired(pName) && !internalMethodName.contains("<init>")
                    && !internalMethodName.contains("<clinit>")) {
                return new CFGNodeMethodVisitor(this, mv, methodNode, internalMethodName);
            }
        }

        return mv;
    }
}

