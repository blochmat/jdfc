package utils;

import data.ProgramVariable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

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

    public boolean isStatic(int access) {
        return (access & ACC_STATIC) != 0;
    }

    public boolean isPrimitiveTypeVar(ProgramVariable var) {
        final List<String> primitives = Arrays.asList("B", "C", "D", "F", "I", "J", "S", "Z");
        return primitives.contains(var.getDescriptor());
    }

    public boolean isInstrumentationRequired(MethodNode methodNode, String internalMethodName) {
        boolean isDefaultConstructor = internalMethodName.equals("<init>: ()V;");
        boolean isSynthetic = ((methodNode.access & ACC_SYNTHETIC) != 0);
        boolean isBridge = ((methodNode.access & ACC_BRIDGE) != 0);
        boolean isAbstract = ((methodNode.access & ACC_ABSTRACT) != 0);
        boolean isJacocoInstrumentation = methodNode.name.contains("$jacoco");
        boolean isLambdaExpression = methodNode.name.contains("$lambda");
        boolean isStaticInitializer = internalMethodName.contains("<clinit>");
        boolean isSourceCodeMethod = !isJacocoInstrumentation
                && !isLambdaExpression
                && !isSynthetic
                && !isBridge
                && !isAbstract
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

    public boolean isCallByValue(String desc) {
        // Primitive type descriptors in ASM are single characters:
        // B (byte), C (char), D (double), F (float), I (int), J (long), S (short), Z (boolean)
        String primitives = "BCDFIJSZ";

        return desc.length() == 1 && primitives.contains(desc);

        // Arrays and Objects are not primitive and so are not call-by-value.
    }

    public List<String> extractParameterTypes(String descriptor) {
        List<String> paramTypes = new ArrayList<>();
        int i = 0;
        boolean isParam = false;

        while (i < descriptor.length()) {
            char ch = descriptor.charAt(i);
            if (ch == '(') {
                isParam = true;
                i++;
                continue;
            }
            if (ch == ')') {
                break;
            }
            if (isParam) {
                StringBuilder type = new StringBuilder();
                if (ch == 'L') {
                    while (descriptor.charAt(i) != ';') {
                        type.append(descriptor.charAt(i++));
                    }
                    type.append(';'); // Include the semicolon in the type
                } else if (ch == '[') {
                    while (descriptor.charAt(i) == '[') {
                        type.append(descriptor.charAt(i++));
                    }
                    if (descriptor.charAt(i) == 'L') {
                        type.append(descriptor.charAt(i++));
                        while (descriptor.charAt(i) != ';') {
                            type.append(descriptor.charAt(i++));
                        }
                        type.append(';'); // Include the semicolon in the type
                    } else {
                        type.append(descriptor.charAt(i));
                    }
                } else {
                    type.append(ch);
                }
                paramTypes.add(type.toString());
            }
            i++;
        }
        return paramTypes;
    }
}
