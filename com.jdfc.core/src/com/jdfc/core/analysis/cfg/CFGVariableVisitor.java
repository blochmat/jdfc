package com.jdfc.core.analysis.cfg;

import static org.objectweb.asm.Opcodes.ASM6;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import com.google.common.collect.Maps;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

/**
 * A class visitor that extracts the information of the local variable table for each method of a
 * class.
 *
 * @see LocalVariableTable
 * @see ClassVisitor
 */
class CFGVariableVisitor extends ClassVisitor {

    private final Map<String, LocalVariableTable> localVariables;
    private final Set<InstanceVariable> instanceVariables;
    private final ClassNode classNode;

    CFGVariableVisitor(final ClassNode pClassNode) {
        super(ASM6);
        localVariables = Maps.newLinkedHashMap();
        instanceVariables = new HashSet<>();
        classNode = pClassNode;
    }

    @Override
    public FieldVisitor visitField(
            final int pAccess,
            final String pName,
            final String pDescriptor,
            final String pSignature,
            final Object pValue) {
        System.out.println("visitField");
        FieldVisitor fv;
        if (cv != null) {
            fv = cv.visitField(pAccess, pName, pDescriptor, pSignature, pValue);
        } else {
            fv = null;
        }
        return new CFGFieldVisitor(this, fv, pAccess, pName, pDescriptor, pSignature, pValue);
    }

    /** {@inheritDoc} */
    @Override
    public MethodVisitor visitMethod(
            final int pAccess,
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
        return new CFGVariableMethodVisitor(
                this, mv, pName, pDescriptor, pSignature, pExceptions);
    }

    /**
     * Returns the information of the local variable tables for each method in a map of method name
     * and {@link LocalVariableTable}.
     *
     * @return A map of method name to {@link LocalVariableTable}
     */
    Map<String, LocalVariableTable> getLocalVariables() {
        return localVariables;
    }

    Set<InstanceVariable> getInstanceVariables() { return instanceVariables; }

    private static class CFGFieldVisitor extends FieldVisitor {

        private final int access;
        private final String name;
        private final String descriptor;
        private final String signature;
        private final Object value;
        private final CFGVariableVisitor classVisitor;

        public CFGFieldVisitor(final CFGVariableVisitor pClassVisitor,
                               final FieldVisitor pFieldVisitor,
                               final int pAccess,
                               final String pName,
                               final String pDescriptor,
                               final String pSignature,
                               final Object pValue) {
            super(ASM6, pFieldVisitor);
            classVisitor = pClassVisitor;
            access = pAccess;
            name = pName;
            descriptor = pDescriptor;
            signature = pSignature;
            value = pValue;
        }

        @Override
        public void visitEnd() {
            final InstanceVariable variable =
                    new InstanceVariable(classVisitor.classNode.name, access, name, descriptor, signature, -1);
            classVisitor.instanceVariables.add(variable);
            super.visitEnd();
        }
    }

    private static class CFGVariableMethodVisitor extends MethodVisitor {

        private final String name;
        private final String descriptor;
        private final String signature;
        private final String[] exceptions;
        private final LocalVariableTable localVariableTable;
        private final CFGVariableVisitor classVisitor;
        private int currentLineNumber;

        CFGVariableMethodVisitor(
                final CFGVariableVisitor pClassVisitor,
                final MethodVisitor pMethodVisitor,
                final String pName,
                final String pDescriptor,
                final String pSignature,
                final String[] pExceptions) {
            super(ASM6, pMethodVisitor);
            classVisitor = pClassVisitor;
            name = pName;
            descriptor = pDescriptor;
            signature = pSignature;
            exceptions = pExceptions;
            localVariableTable = new LocalVariableTable();
            currentLineNumber = -1;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            currentLineNumber = line;
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitFieldInsn(int pOpcode, String pOwner, String pName, String pDescription) {
            if (pOpcode == PUTFIELD) {
                Set<InstanceVariable> instanceVariables = classVisitor.getInstanceVariables();
                for (InstanceVariable instanceVariable : instanceVariables) {
                    if (instanceVariable.getOwner().equals(pOwner)
                            && instanceVariable.getName().equals(pName)
                            && instanceVariable.getDescriptor().equals(pDescription)) {
                        instanceVariable.setLineNumber(currentLineNumber);
                    }
                }
            }
            super.visitFieldInsn(pOpcode, pOwner, pName, pDescription);
        }

        /** {@inheritDoc} */
        @Override
        public void visitLocalVariable(
                final String pName,
                final String pDescriptor,
                final String pSignature,
                final Label pStart,
                final Label pEnd,
                final int pIndex) {
            final LocalVariable variable = new LocalVariable(pName, pDescriptor, pSignature, pIndex);
            localVariableTable.addEntry(pIndex, variable);
            super.visitLocalVariable(pName, pDescriptor, pSignature, pStart, pEnd, pIndex);
        }

        /** {@inheritDoc} */
        @Override
        public void visitEnd() {
            final String methodName =
                    CFGCreator.computeInternalMethodName(name, descriptor, signature, exceptions);
            classVisitor.localVariables.put(methodName, localVariableTable);
        }
    }
}
