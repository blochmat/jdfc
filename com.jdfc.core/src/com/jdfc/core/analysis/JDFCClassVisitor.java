package com.jdfc.core.analysis;

import com.google.common.collect.Maps;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.data.LocalVariable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

public abstract class JDFCClassVisitor extends ClassVisitor {

    public final ClassNode classNode;
    public final ClassExecutionData classExecutionData;
    public final Map<String, Map<Integer, LocalVariable>> localVariableTables;
    public final String jacocoMethodName = "$jacoco";

    public JDFCClassVisitor(final int pApi,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData) {
        super(pApi);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
        localVariableTables = new HashMap<>();
    }

    public JDFCClassVisitor(final int pApi,
                            final ClassVisitor pClassVisitor,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData) {
        super(pApi, pClassVisitor);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
        localVariableTables = new HashMap<>();
    }

    public JDFCClassVisitor(final int pApi,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData,
                            final Map<String, Map<Integer, LocalVariable>> pLocalVariableTables) {
        super(pApi);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
        localVariableTables = pLocalVariableTables;
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        final MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        } else {
            mv = null;
        }

        return mv;
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

    protected boolean isInstrumentationRequired(String pString) {
        return !pString.contains(jacocoMethodName);
    }

    /**
     * Returns the information of the local variable tables for each method in a map of method name
     * and {@link LocalVariableTable}.
     *
     * @return A map of method name to {@link LocalVariableTable}
     */
    public Map<String, Map<Integer, LocalVariable>> getLocalVariableTables() {
        return localVariableTables;
    }
}
