package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

/**
 * Represents a program variable that is identified by its name and type.
 */
public class ProgramVariable implements Comparable<Object>{

    private final String owner;
    private final String name;
    private final String descriptor;
    private final int instructionIndex;
    private final int lineNumber;

    private ProgramVariable(
            final String pOwner, final String pName, final String pType, final int pInstructionIndex, final int pLineNumber) {
        owner = pOwner;
        name = pName;
        descriptor = pType;
        instructionIndex = pInstructionIndex;
        lineNumber = pLineNumber;
    }

    public static ProgramVariable create(
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
    public String getDescriptor() {
        return descriptor;
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
                && Objects.equals(descriptor, that.descriptor)
                && instructionIndex == that.instructionIndex
                && lineNumber == that.lineNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(owner, name, descriptor, instructionIndex, lineNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ProgramVariable %s: %s (Idx: %d, LNr: %d, Owner: %s)",
                name, descriptor, instructionIndex, lineNumber, owner);
    }

    public static String encode(ProgramVariable pVar) {
        return String.format("%s,%s,%s,%s,%s", pVar.owner, pVar.name, pVar.descriptor, pVar.instructionIndex, pVar.lineNumber);
    }

    public static ProgramVariable decode (String pEncoded) {
        String[] props = pEncoded.split(",");
        return ProgramVariable.create(props[0], props[1], props[2], Integer.parseInt(props[3]), Integer.parseInt(props[4]));
    }

    @Override
    public int compareTo(Object pOther) {
        if(pOther == null) {
            throw new NullPointerException("Can't compare to null.");
        }
        ProgramVariable that = (ProgramVariable) pOther;

        if(this.equals(that)) {
            return 0;
        }
        if(this.getLineNumber() == that.getLineNumber()) {
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
