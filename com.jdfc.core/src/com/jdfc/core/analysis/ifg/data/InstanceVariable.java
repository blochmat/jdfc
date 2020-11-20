package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

public class InstanceVariable implements Comparable<Object> {

    private final ProgramVariable holder;
    private final String method;
    private final int instructionIndex;
    private final int lineNumber;
    private final boolean isDefinition;
    private final String owner;
    private final String name;
    private final String descriptor;

    private InstanceVariable(final String pOwner,
                             final ProgramVariable pHolder,
                             final String pMethod,
                             final String pName,
                             final String pDescriptor,
                             final int pInstructionIndex,
                             final int pLineNumber,
                             final boolean pIsDefinition) {
        owner = pOwner;
        name = pName;
        descriptor = pDescriptor;
        method = pMethod;
        holder = pHolder;
        instructionIndex = pInstructionIndex;
        lineNumber = pLineNumber;
        isDefinition = pIsDefinition;
    }

    public static InstanceVariable create(final String pOwner,
                                          final ProgramVariable pHolder,
                                          final String pMethod,
                                          final String pName,
                                          final String pDescriptor,
                                          final int pInstructionIndex,
                                          final int pLineNumber,
                                          final boolean pIsDefinition) {
        return new InstanceVariable(pOwner, pHolder, pMethod, pName, pDescriptor, pInstructionIndex, pLineNumber, pIsDefinition);
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

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getOwner() {
        return owner;
    }


    public ProgramVariable convertToProgramVariable() {
        String method = null;
        if(holder != null) {
            method = holder.getMethod();
        }
        return ProgramVariable.create(
                this.owner,
                this.name,
                this.descriptor,
                method,
                this.instructionIndex,
                this.lineNumber,
                false,
                this.isDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, method, name, descriptor, lineNumber, isDefinition);
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
                && Objects.equals(name, that.name)
                && Objects.equals(descriptor, that.descriptor)
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
                + ", name='"
                + name
                + '\''
                + ", descriptor='"
                + descriptor
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

