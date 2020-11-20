package com.jdfc.core.analysis.ifg.data;

import com.jdfc.core.analysis.JDFCClassVisitor;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.CFGCreator;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM8;

public class InstanceVariableClassVisitor extends JDFCClassVisitor {

    public InstanceVariableClassVisitor(ClassNode pClassNode, ClassExecutionData pClassExecutionData, Map<String, LocalVariableTable> pLocalVariableTables) {
        super(ASM8, pClassNode, pClassExecutionData, pLocalVariableTables);
    }

    @Override
    public FieldVisitor visitField(
            final int pAccess,
            final String pName,
            final String pDescriptor,
            final String pSignature,
            final Object pValue) {
        FieldVisitor fv;
        if (cv != null) {
            fv = cv.visitField(pAccess, pName, pDescriptor, pSignature, pValue);
        } else {
            fv = null;
        }
        if (isInstrumentationRequired(pName)) {
            return new InstanceVariableFieldVisitor(this, fv, pAccess, pName, pDescriptor, pSignature, pValue);
        }
        return fv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        final MethodVisitor mv = super.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        final MethodNode methodNode = getMethodNode(pName, pDescriptor);
        final String internalMethodName = CFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        final LocalVariableTable localVariableTable = localVariableTables.get(internalMethodName);
        if (methodNode != null && isInstrumentationRequired(pName)) {
            return new InstanceVariableMethodVisitor(
                    this, mv, methodNode, internalMethodName, localVariableTable);
        }
        return mv;
    }
}
