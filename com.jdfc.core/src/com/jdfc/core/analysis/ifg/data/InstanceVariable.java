package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

public class InstanceVariable extends Field implements Comparable<Object> {

    private final ProgramVariable holder;
    private final String method;
    private final int instructionIndex;
    private final int lineNumber;
    private final boolean isDefinition;

    private InstanceVariable(final String pOwner,
                             final ProgramVariable pHolder,
                             final String pMethod,
                             final int pAccess,
                             final String pName,
                             final String pDescriptor,
                             final String pSignature,
                             final int pInstructionIndex,
                             final int pLineNumber,
                             final boolean pIsDefinition) {
        super(pOwner, pAccess, pName, pDescriptor, pSignature);
        method = pMethod;
        holder = pHolder;
        instructionIndex = pInstructionIndex;
        lineNumber = pLineNumber;
        isDefinition = pIsDefinition;
    }

    public static InstanceVariable create(final String pOwner,
                                          final ProgramVariable pHolder,
                                          final String pMethod,
                                          final int pAccess,
                                          final String pName,
                                          final String pDescriptor,
                                          final String pSignature,
                                          final int pInstructionIndex,
                                          final int pLineNumber,
                                          final boolean pIsDefinition) {
        return new InstanceVariable(pOwner, pHolder, pMethod, pAccess, pName, pDescriptor, pSignature, pInstructionIndex, pLineNumber, pIsDefinition);
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

    public String getMethod() {
        return method;
    }

    public boolean isDefinition() {
        return isDefinition;
    }

    public ProgramVariable convertToProgramVariable() {
        return ProgramVariable.create(
                this.owner,
                this.name,
                this.descriptor,
                this.holder.getMethod(),
                this.instructionIndex,
                this.lineNumber,
                false,
                this.isDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, method, access, name, descriptor, signature, lineNumber, isDefinition);
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
                && method.equals(that.method)
                && access == that.access
                && Objects.equals(name, that.name)
                && Objects.equals(descriptor, that.descriptor)
                && Objects.equals(signature, that.signature)
                && Objects.equals(holder, that.holder)
                && instructionIndex == that.instructionIndex
                && lineNumber == that.lineNumber
                && Boolean.compare(isDefinition, that.isDefinition) == 0;
    }

    @Override
    public String toString() {
        return "InstanceVariable{"
                + "owner='"
                + owner
                + '\''
                + ", method='"
                + method
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
                + ", signature='"
                + signature
                + '\''
                + ", instructionIndex= "
                + instructionIndex
                + ", lineNumber= "
                + lineNumber
                + ", isDefinition='"
                + isDefinition
                + '\''
                + ", holderInformation='"
                + holder
                + '\''
                + '}';
    }

    @Override
    public int compareTo(Object pOther) {
        if (pOther == null) {
            throw new NullPointerException("Can't compare to null.");
        }
        InstanceVariable that = (InstanceVariable) pOther;

        if (this.equals(that)) {
            return 0;
        }
        if (this.getLineNumber() == that.getLineNumber()) {
            if (this.getInstructionIndex() < that.getInstructionIndex()) {
                return -1;
            } else {
                return 1;
            }
        } else {
            if (this.getLineNumber() < that.getLineNumber()) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}

