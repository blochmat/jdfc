package instr.classVisitors;

import data.ClassExecutionData;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import utils.JDFCUtils;

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
        if(methodNode.name.contains("<init>")) {
            String debug = String.format("%s::%s -> synth: %b, bridge: %b", classExecutionData.getRelativePath(), internalMethodName, ((methodNode.access & Opcodes.ACC_SYNTHETIC) == 0), ((methodNode.access & Opcodes.ACC_BRIDGE) == 0));
            JDFCUtils.logThis(debug, "synth_check");
        }

        boolean isSynthetic = !((methodNode.access & Opcodes.ACC_SYNTHETIC) == 0);
        boolean isBridge = !((methodNode.access & Opcodes.ACC_BRIDGE) == 0);
        boolean isJacocoInstrumentation = methodNode.name.contains("$jacoco");
        boolean isLambdaExpression = methodNode.name.contains("$lambda");

        boolean isDefaultConstructor = internalMethodName.equals("<init>: ()V;") && isSynthetic && isBridge;
        boolean isSourceCodeMethod = !isJacocoInstrumentation && !isLambdaExpression && !isSynthetic && !isBridge;

        if(methodNode.name.contains("<init>")) {
            String debug = String.format("%s::%s -> default: %b, sourceCode: %b", classExecutionData.getRelativePath(), internalMethodName, isDefaultConstructor, isSourceCodeMethod);
            JDFCUtils.logThis(debug, "synth_check");
        }

        return  isDefaultConstructor || isSourceCodeMethod;
    }
}
