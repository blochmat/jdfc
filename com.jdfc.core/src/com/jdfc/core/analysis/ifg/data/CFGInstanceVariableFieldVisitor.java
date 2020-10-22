package com.jdfc.core.analysis.ifg.data;

import org.objectweb.asm.FieldVisitor;

import static org.objectweb.asm.Opcodes.ASM6;

class CFGInstanceVariableFieldVisitor extends FieldVisitor {

    private final int access;
    private final String name;
    private final String descriptor;
    private final String signature;
    private final Object value;
    private final CFGInstanceVariableClassVisitor classVisitor;

    public CFGInstanceVariableFieldVisitor(final CFGInstanceVariableClassVisitor pClassVisitor,
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
                InstanceVariable.create(classVisitor.classNode.name, null, access, name, descriptor, signature, -1);
        classVisitor.classExecutionData.getInstanceVariables().add(variable);
        super.visitEnd();
    }
}
