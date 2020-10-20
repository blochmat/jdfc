package com.jdfc.core.analysis.ifg.data;

import static org.objectweb.asm.Opcodes.ASM6;

import com.jdfc.core.analysis.JDFCClassVisitor;
import com.jdfc.core.analysis.data.ClassExecutionData;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

/**
 * A class visitor that extracts the information of the local variable table for each method of a
 * class.
 *
 * @see LocalVariableTable
 * @see ClassVisitor
 */
public class CFGVariableClassVisitor extends JDFCClassVisitor {

    public CFGVariableClassVisitor(final ClassNode pClassNode, final ClassExecutionData pClassExecutionData) {
        super(ASM6, pClassNode, pClassExecutionData);
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
            return new CFGVariableFieldVisitor(this, fv, pAccess, pName, pDescriptor, pSignature, pValue);
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
        final MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        } else {
            mv = null;
        }
        if (isInstrumentationRequired(pName)) {
            return new CFGVariableMethodVisitor(
                    this, mv, pName, pDescriptor, pSignature, pExceptions);
        }
        return mv;
    }
}
