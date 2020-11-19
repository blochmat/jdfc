package com.jdfc.core.analysis.ifg;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.data.InstanceVariableClassVisitor;
import com.jdfc.core.analysis.ifg.data.LocalVariableClassVisitor;
import com.jdfc.core.analysis.ifg.data.LocalVariableTable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.Map;

/**
 * Creates {@link CFG}s for each method of a class file.
 */
public class CFGCreator {

    private CFGCreator() {
    }

    /**
     * Creates the {@link CFG}s for each method of a class and returns a map of method name to {@link
     * CFG}.
     *
     * <p>The key of the map is in the format
     *
     * <pre>
     *   name: descriptor[; signature][; exceptions]
     * </pre>
     * <p>
     * Hence, a default constructor will have the key <code>&lt;init&gt;: ()V</code> and a method
     * "foo" that takes an int and a String and returns a double array will have the key <code> foo:
     * (ILjava/lang/String;)[D</code>.
     *
     * <p>This method is the only method to start the generation of {@link CFG}s. The full creation
     * process of the graphs is then done internally and only the final graphs will be given back to
     * the user.
     *
     * @param pClassReader The class reader instance on the class
     * @param pClassNode   An empty class node to start the analysis with
     * @return A map of method name and {@link CFG}
     */
    public static void createCFGsForClass(final ClassReader pClassReader,
                                                      final ClassNode pClassNode,
                                                      final ClassExecutionData pClassExecutionData) {
        Preconditions.checkNotNull(
                pClassReader, "We need a non-null class reader to generate CFGs from.");
        Preconditions.checkNotNull(pClassNode, "We need a non-null class node to generate CFGs from.");
        Preconditions.checkNotNull(
                pClassExecutionData, "We need a non-null class execution data to generate CFGs from.");

        // Get local variable information
        final LocalVariableClassVisitor localVariableVisitor =
                new LocalVariableClassVisitor(pClassNode, pClassExecutionData);
        pClassReader.accept(localVariableVisitor, 0);

        final Map<String, LocalVariableTable> localVariableTables =
                localVariableVisitor.getLocalVariableTables();

        // Get instance variable information
        final InstanceVariableClassVisitor instanceVariableVisitor =
                new InstanceVariableClassVisitor(pClassNode, pClassExecutionData, localVariableTables);
        pClassReader.accept(instanceVariableVisitor, 0);

        // Create method cfgs
        final Map<String, CFG> methodCFGs = Maps.newLinkedHashMap();
        final CFGCreatorClassVisitor cfgCreatorClassVisitor =
                new CFGCreatorClassVisitor(pClassNode, pClassExecutionData, methodCFGs, localVariableTables);
        pClassReader.accept(cfgCreatorClassVisitor, 0);
    }

    /**
     * Computes the internal method name representation that is used, for example, in the map emitted
     * by {@link CFGCreator#createCFGsForClass(ClassReader, ClassNode, ClassExecutionData)}.
     *
     * @param pMethodName The name of the method
     * @param pDescriptor The method's descriptor
     * @param pSignature  The method's signature
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
        if (pExceptions != null && pExceptions.length != 0) {
            result.append("; ").append(Arrays.toString(pExceptions));
        }
        return result.toString();
    }
}
