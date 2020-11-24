package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

public class InstanceVariable implements Comparable<Object> {

    private final int instructionIndex;
    private final int lineNumber;
    private final boolean isDefinition;
    private final String owner;
    private final String name;
    private final String descriptor;

    private InstanceVariable(final String pOwner,
                             final String pName,
                             final String pDescriptor,
                             final int pInstructionIndex,
                             final int pLineNumber,
                             final boolean pIsDefinition) {
        owner = pOwner;
        name = pName;
        descriptor = pDescriptor;
        instructionIndex = pInstructionIndex;
        lineNumber = pLineNumber;
        isDefinition = pIsDefinition;
    }

    public static InstanceVariable create(final String pOwner,
                                          final String pName,
                                          final String pDescriptor,
                                          final int pInstructionIndex,
                                          final int pLineNumber,
                                          final boolean pIsDefinition) {
        return new InstanceVariable(pOwner, pName, pDescriptor, pInstructionIndex, pLineNumber, pIsDefinition);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getInstructionIndex() {
        return instructionIndex;
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
        return ProgramVariable.create(
                this.owner,
                this.name,
                this.descriptor,
                this.instructionIndex,
                this.lineNumber,
                this.isDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, descriptor, lineNumber, isDefinition);
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
                && Objects.equals(name, that.name)
                && Objects.equals(descriptor, that.descriptor)
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

