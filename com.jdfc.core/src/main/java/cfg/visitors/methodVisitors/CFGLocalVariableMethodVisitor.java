package cfg.visitors.methodVisitors;

import cfg.data.LocalVariable;
import cfg.visitors.classVisitors.CFGLocalVariableClassVisitor;
import instr.methodVisitors.JDFCMethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.ASM5;

public class CFGLocalVariableMethodVisitor extends JDFCMethodVisitor {

    private final Logger logger = LoggerFactory.getLogger(CFGLocalVariableMethodVisitor.class);

    public CFGLocalVariableMethodVisitor(
            final CFGLocalVariableClassVisitor pClassVisitor,
            final MethodVisitor pMethodVisitor,
            final MethodNode pMethodNode,
            final String pInternalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(
            final String pName,
            final String pDescriptor,
            final String pSignature,
            final Label pStart,
            final Label pEnd,
            final int pIndex) {
        logger.debug("visitLocalVariable");
        super.visitLocalVariable(pName, pDescriptor, pSignature, pStart, pEnd, pIndex);
        final LocalVariable variable = new LocalVariable(pName, pDescriptor, pSignature, pIndex);
        localVariableTable.put(pIndex, variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        logger.debug("visitEnd");
        // TODO
        if(!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
            classVisitor.classExecutionData.getMethodByInternalName(internalMethodName)
                    .setLocalVariableTable(localVariableTable);
        }
    }
}
