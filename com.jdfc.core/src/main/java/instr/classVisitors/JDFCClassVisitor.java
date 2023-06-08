package instr.classVisitors;

import data.ClassExecutionData;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JDFCClassVisitor extends ClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(JDFCClassVisitor.class);
    public final ClassNode classNode;

    public final ClassExecutionData classExecutionData;

    public JDFCClassVisitor(final int pApi,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData) {
        super(pApi);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
    }

    public JDFCClassVisitor(final int pApi,
                            final ClassVisitor pClassVisitor,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData) {
        super(pApi, pClassVisitor);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
    }

    public MethodNode getMethodNode(final String pName,
                                    final String pDescriptor) {
        logger.debug("getMethodNode");
        for (MethodNode node : classNode.methods) {
            if (node.name.equals(pName) && node.desc.equals(pDescriptor)) {
                return node;
            }
        }
        return null;
    }

    protected boolean isInstrumentationRequired(MethodNode methodNode) {
        logger.debug("isInstrumentationRequired");
        return !methodNode.name.contains("$jacoco") // no jacoco method
                && !methodNode.name.contains("$lambda") // no lambda expression
                && ((methodNode.access & Opcodes.ACC_SYNTHETIC) == 0) // no synthetic method
                && ((methodNode.access & Opcodes.ACC_BRIDGE) == 0); // no bridge method
    }

    public ClassNode getClassNode() {
        logger.debug("getClassNode");
        return classNode;
    }

}
