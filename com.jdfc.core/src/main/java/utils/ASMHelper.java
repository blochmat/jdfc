package utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

public class ASMHelper {

    private final Logger logger = LoggerFactory.getLogger(ASMHelper.class);

    /**
     * Computes the internal method name representation from the information provided
     * by {@link org.objectweb.asm.ClassVisitor}
     *
     * @param pMethodName The name of the method
     * @param pDescriptor The method's descriptor
     * @param pSignature  The method's signature
     * @param pExceptions An array of exceptions thrown by the method
     * @return The internal method name representation
     */
    public String computeInternalMethodName(
            final String pMethodName,
            final String pDescriptor,
            final String pSignature,
            final String[] pExceptions) {
        final StringBuilder result = new StringBuilder();
        result.append(pMethodName);
        result.append(": ");

        if (pSignature != null) {
            result.append(pSignature);
            if (!pSignature.endsWith(";")){
                result.append(";");
            }
        } else {
            result.append(pDescriptor);
            if(!pDescriptor.endsWith(";")) {
                result.append(";");
            }
        }

        if (pExceptions != null && pExceptions.length != 0) {
            result.append(" ").append(Arrays.toString(pExceptions));
        }
        return result.toString();
    }

    public boolean isInstrumentationRequired(MethodNode methodNode, String internalMethodName) {
        boolean isDefaultConstructor = internalMethodName.equals("<init>: ()V;");
        boolean isSynthetic = ((methodNode.access & Opcodes.ACC_SYNTHETIC) != 0);
        boolean isBridge = ((methodNode.access & Opcodes.ACC_BRIDGE) != 0);
        boolean isJacocoInstrumentation = methodNode.name.contains("$jacoco");
        boolean isLambdaExpression = methodNode.name.contains("$lambda");
        boolean isStaticInitializer = internalMethodName.contains("<clinit>");
        boolean isSourceCodeMethod = !isJacocoInstrumentation
                && !isLambdaExpression
                && !isSynthetic
                && !isBridge
                && !isStaticInitializer;

        return  isDefaultConstructor || isSourceCodeMethod;
    }

    public MethodNode getMethodNode(ClassNode classNode,
                                    String name,
                                    String descriptor,
                                    String signature) {
        for (MethodNode node : classNode.methods) {
            if (Objects.equals(node.name, name)
                    && Objects.equals(node.desc, descriptor)
                    && Objects.equals(node.signature, signature)) {
                return node;
            }
        }
        return null;
    }
}
