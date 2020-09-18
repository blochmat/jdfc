package com.jdfc.core.analysis.cfg;

import java.util.Objects;

/**
 * Represents a program variable that is identified by its name and type.
 */
public class ProgramVariable {

    private final String owner;
    private final String name;
    private final String type;
    private final int instructionIndex;
    private final int lineNumber;

    public ProgramVariable(
            final String pOwner, final String pName, final String pType, final int pInstructionIndex, final int pLineNumber) {
        owner = pOwner;
        name = pName;
        type = pType;
        instructionIndex = pInstructionIndex;
        lineNumber = pLineNumber;
    }

    static ProgramVariable create(
            final String pOwner, final String pName, final String pType, final int pInstructionIndex, final int pLineNumber) {
        return new ProgramVariable(pOwner, pName, pType, pInstructionIndex, pLineNumber);
    }

    /**
     * Returns the variable's name.
     *
     * @return The variable's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the variable's type.
     *
     * @return The variable's type
     */
    public String getType() {
        return type;
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (pOther == null || getClass() != pOther.getClass()) {
            return false;
        }
        final ProgramVariable that = (ProgramVariable) pOther;
        return Objects.equals(owner, that.owner)
                && Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && instructionIndex == that.instructionIndex
                && lineNumber == that.lineNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(owner, name, type, instructionIndex, lineNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ProgramVariable %s: %s (Idx: %d, LNr: %d, Owner: %s)",
                name, type, instructionIndex, lineNumber, owner);
    }
}
