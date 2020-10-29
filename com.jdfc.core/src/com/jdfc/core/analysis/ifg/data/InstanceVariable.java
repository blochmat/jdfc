package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

public class InstanceVariable extends Field{

    private final ProgramVariable holder;
    private final int instructionIndex;
    private final int lineNumber;

    private InstanceVariable(final String pOwner,
                             final ProgramVariable pHolder,
                             final int pAccess,
                             final String pName,
                             final String pDescriptor,
                             final String pSignature,
                             final int pInstructionIndex,
                             final int pLineNumber) {
        super(pOwner, pAccess, pName, pDescriptor, pSignature);
        holder = pHolder;
        instructionIndex = pInstructionIndex;
        lineNumber = pLineNumber;
    }

    public static InstanceVariable create(final String pOwner,
                                          final ProgramVariable pHolder,
                                          final int pAccess,
                                          final String pName,
                                          final String pDescriptor,
                                          final String pSignature,
                                          final int pInstructionIndex,
                                          final int pLineNumber) {
        return new InstanceVariable(pOwner, pHolder, pAccess, pName, pDescriptor, pSignature, pInstructionIndex, pLineNumber);
    }

    public ProgramVariable getHolder() {
        return holder;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, access, name, descriptor, signature, lineNumber);
    }

    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (pOther == null || getClass() != pOther.getClass()) {
            return false;
        }
        final InstanceVariable that = (InstanceVariable) pOther;
        return Objects.equals(owner, that.owner)
                && access == that.access
                && Objects.equals(name, that.name)
                && Objects.equals(descriptor, that.descriptor)
                && Objects.equals(signature, that.signature)
                && holder.equals(that.holder)
                && instructionIndex == that.instructionIndex
                && lineNumber == that.lineNumber;
    }

    @Override
    public String toString() {
        return "InstanceVariable{"
                + "owner='"
                + owner
                + '\''
                + ", access='"
                + access
                + '\''
                + ", name='"
                + name
                + '\''
                + ", descriptor='"
                + descriptor
                + '\''
                + ", signature="
                + signature
                + '\''
                + ", instructionIndex="
                + instructionIndex
                + '\''
                + ", lineNumber='"
                + lineNumber
                + '\''
                + ", holderInformation='"
                + holder
                + '\''
                + '}';
    }
}

