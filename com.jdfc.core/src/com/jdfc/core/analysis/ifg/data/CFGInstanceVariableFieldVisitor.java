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
        if (fv != null) {
            fv.visitEnd();
        } else {
            super.visitEnd();
        }

        System.out.println("INSTANCEVAR: " + name);
        System.out.println("ACCESS: " + access);
        System.out.println("DESCRIPTOR: " + descriptor);
        final Field variable =
                Field.create(classVisitor.classNode.name, access, name, descriptor, signature);
        classVisitor.classExecutionData.getFields().add(variable);
    }
}
