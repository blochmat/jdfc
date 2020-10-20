package com.jdfc.core.analysis;

import com.google.common.collect.Maps;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.data.LocalVariableTable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

public abstract class JDFCClassVisitor extends ClassVisitor {

    public final ClassNode classNode;
    public final ClassExecutionData classExecutionData;
    public final Map<String, LocalVariableTable> localVariableTables;
    public final String jacocoMethodName = "$jacoco";

    public JDFCClassVisitor(final int pApi,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData) {
        super(pApi);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
        localVariableTables = Maps.newLinkedHashMap();
    }

    public JDFCClassVisitor(final int pApi,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData,
                            final Map<String, LocalVariableTable> pLocalVariableTables) {
        super(pApi);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
        localVariableTables = pLocalVariableTables;
    }

    public MethodNode getMethodNode(String pName) {
        for (MethodNode node : classNode.methods) {
            if (node.name.equals(pName)) {
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
    public Map<String, LocalVariableTable> getLocalVariableTables() {
        return localVariableTables;
    }
}
