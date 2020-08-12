package com.jdfc.core.analysis.cfg;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/** Creates {@link CFG}s for each method of a class file. */
public class CFGCreator {

    private CFGCreator() {}

    /**
     * Creates the {@link CFG}s for each method of a class and returns a map of method name to {@link
     * CFG}.
     *
     * <p>The key of the map is in the format
     *
     * <pre>
     *   name: descriptor[; signature][; exceptions]
     * </pre>
     *
     * Hence, a default constructor will have the key <code>&lt;init&gt;: ()V</code> and a method
     * "foo" that takes an int and a String and returns a double array will have the key <code> foo:
     * (ILjava/lang/String;)[D</code>.
     *
     * <p>This method is the only method to start the generation of {@link CFG}s. The full creation
     * process of the graphs is then done internally and only the final graphs will be given back to
     * the user.
     *
     * @param pClassReader The class reader instance on the class
     * @param pClassNode An empty class node to start the analysis with
     * @return A map of method name and {@link CFG}
     */
    public static Map<String, CFG> createCFGsForClass(
            final ClassReader pClassReader, final ClassNode pClassNode) {
        Preconditions.checkNotNull(
                pClassReader, "We need a non-null class reader to generate CFGs from.");
        Preconditions.checkNotNull(pClassNode, "We need a non-null class node to generate CFGs from.");

        final CFGLocalVariableTableVisitor localVariableTableVisitor =
                new CFGLocalVariableTableVisitor();
        pClassReader.accept(localVariableTableVisitor, 0);
        final Map<String, LocalVariableTable> localVariableTables =
                localVariableTableVisitor.getLocalVariables();

        pClassReader.accept(pClassNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        final Map<String, CFG> methodCFGs = Maps.newLinkedHashMap();
        for (MethodNode methodNode : pClassNode.methods) {
            final String localVariableTableKey = computeInternalMethodName(methodNode);
            final LocalVariableTable localVariableTable = localVariableTables.get(localVariableTableKey);
            final Type[] parameterTypes = Type.getArgumentTypes(methodNode.desc);
            final MethodCFGCreator methodCFGCreator =
                    new MethodCFGCreator(methodNode, localVariableTable, parameterTypes);
            final CFG cfg = methodCFGCreator.create();
            cfg.calculateReachingDefinitions();
            methodCFGs.put(localVariableTableKey, cfg);
        }
        return Collections.unmodifiableMap(methodCFGs);
    }

    /**
     * Computes the internal method name representation that is used, for example, in the map emitted
     * by {@link CFGCreator#createCFGsForClass(ClassReader, ClassNode)}.
     *
     * @param pMethodNode The method node to get the information from
     * @return The internal method name representation
     */
    public static String computeInternalMethodName(final MethodNode pMethodNode) {
        final String methodName = pMethodNode.name;
        final String descriptor = pMethodNode.desc;
        final String signature = pMethodNode.signature;
        final String[] exceptions;
        if (pMethodNode.exceptions.size() > 0) {
            exceptions = (String[]) pMethodNode.exceptions.toArray(new String[0]);
        } else {
            exceptions = null;
        }
        return computeInternalMethodName(methodName, descriptor, signature, exceptions);
    }

    /**
     * Computes the internal method name representation that is used, for example, in the map emitted
     * by {@link CFGCreator#createCFGsForClass(ClassReader, ClassNode)}.
     *
     * @param pMethodName The name of the method
     * @param pDescriptor The method's descriptor
     * @param pSignature The method's signature
     * @param pExceptions An array of exceptions thrown by the method
     * @return The internal method name representation
     */
    public static String computeInternalMethodName(
            final String pMethodName,
            final String pDescriptor,
            final String pSignature,
            final String[] pExceptions) {
        final StringBuilder result = new StringBuilder();
        result.append(pMethodName);
        result.append(": ");
        result.append(pDescriptor);
        if (pSignature != null) {
            result.append("; ").append(pSignature);
        }
        if (pExceptions != null) {
            result.append("; ").append(Arrays.toString(pExceptions));
        }
        return result.toString();
    }

}
