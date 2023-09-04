package instr.classVisitors;

import data.ClassExecutionData;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class JDFCClassVisitor extends ClassVisitor {

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
        for (MethodNode node : classNode.methods) {
            if (node.name.equals(pName) && node.desc.equals(pDescriptor)) {
                return node;
            }
        }
        return null;
    }

    protected boolean isInstrumentationRequired(MethodNode methodNode, String internalMethodName) {
        boolean isDefaultConstructor = internalMethodName.equals("<init>: ()V;");
        boolean isSynthetic = ((methodNode.access & Opcodes.ACC_SYNTHETIC) != 0);
        boolean isBridge = ((methodNode.access & Opcodes.ACC_BRIDGE) != 0);
        boolean isJacocoInstrumentation = methodNode.name.contains("$jacoco");
        boolean isLambdaExpression = methodNode.name.contains("$lambda");
        boolean isStaticInitializer = internalMethodName.contains("<clinit>");
        boolean isGenericMethod = methodNode.signature != null;
        boolean isSourceCodeMethod = !isJacocoInstrumentation
                && !isLambdaExpression
                && !isSynthetic
                && !isBridge
                && !isStaticInitializer
                && !isGenericMethod;

        return  isDefaultConstructor || isSourceCodeMethod;
    }
}
