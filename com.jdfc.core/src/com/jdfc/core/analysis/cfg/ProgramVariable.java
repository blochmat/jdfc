package com.jdfc.core.analysis.cfg;

import java.util.Objects;

/** Represents a program variable that is identified by its name and type. */
public class ProgramVariable {

    private final String name;
    private final String type;
    private final int instructionIndex;

    public ProgramVariable(final String pName, final String pType, final int pInstructionIndex) {
        name = pName;
        type = pType;
        instructionIndex = pInstructionIndex;
    }

    static ProgramVariable create(
            final String pName, final String pType, final int pInstructionIndex) {
        return new ProgramVariable(pName, pType, pInstructionIndex);
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

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object pOther) {
        if (this == pOther) {
            return true;
        }
        if (pOther == null || getClass() != pOther.getClass()) {
            return false;
        }
        final ProgramVariable that = (ProgramVariable) pOther;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && instructionIndex == that.instructionIndex;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(name, type, instructionIndex);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("ProgramVariable %s: %s (Idx: %d)", name, type, instructionIndex);
    }
}
