package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

/**
 * Represents a program variable that is identified by its name and type.
 */
public class ProgramVariable implements Comparable<Object> {

    private final String owner;
    private final String name;
    private final String descriptor;
    private final int instructionIndex;
    private final int lineNumber;
    private boolean isReference;
    private boolean isDefinition;

    private ProgramVariable(final String pOwner,
                            final String pName,
                            final String pDescriptor,
                            final int pInstructionIndex,
                            final int pLineNumber,
                            final boolean pIsReference,
                            final boolean pIsDefinition) {
        owner = pOwner;
        name = pName;
        descriptor = pDescriptor;
        instructionIndex = pInstructionIndex;
        lineNumber = pLineNumber;
        isReference = pIsReference;
        isDefinition = pIsDefinition;
    }

    public static ProgramVariable create(final String pOwner,
                                         final String pName,
                                         final String pDescriptor,
                                         final int pInstructionIndex,
                                         final int pLineNumber,
                                         final boolean pIsReference,
                                         final boolean pIsDefinition) {
        return new ProgramVariable(pOwner, pName, pDescriptor, pInstructionIndex, pLineNumber, pIsReference, pIsDefinition);
    }

    public ProgramVariable clone() throws CloneNotSupportedException {
        return (ProgramVariable) super.clone();
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

    public void setReference(boolean reference) {
        isReference = reference;
    }

    public boolean isReference() {
        return isReference;
    }

    public void setDefinition(boolean definition) {
        isDefinition = definition;
    }

    public boolean isDefinition() {
        return isDefinition;
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
                && (instructionIndex == that.instructionIndex)
                && (lineNumber == that.lineNumber)
                && Boolean.compare(isReference, that.isReference) == 0
                && Boolean.compare(isDefinition, that.isDefinition) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(owner, name, descriptor, instructionIndex, lineNumber, isReference, isDefinition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ProgramVariable %s: %s (Idx: %d, LNr: %d, Owner: %s, isReference: %s, isDefinition: %s)",
                name, descriptor, instructionIndex, lineNumber, owner, isReference, isDefinition);
    }

    public static String encode(ProgramVariable pVar) {
        return String.format("%s,%s,%s,%s,%s,%s,%s",
                pVar.owner, pVar.name, pVar.descriptor, pVar.instructionIndex, pVar.lineNumber, pVar.isReference, pVar.isDefinition);
    }

    public static ProgramVariable decode(String pEncoded) {
        String[] props = pEncoded.split(",");
        String owner = (props[0].equals("null") ? null : props[0]);
        String name = (props[1].equals("null") ? null : props[1]);
        String descriptor = (props[2].equals("null") ? null : props[2]);
        int instructionIndex = Integer.parseInt(props[3]);
        int lineNumber = Integer.parseInt(props[4]);
        boolean isReference = Boolean.parseBoolean(props[5]);
        boolean isDefinition = Boolean.parseBoolean(props[6]);

        return ProgramVariable.create(owner, name, descriptor, instructionIndex, lineNumber, isReference, isDefinition);
    }

    @Override
    public int compareTo(Object pOther) {
        if (pOther == null) {
            throw new NullPointerException("Can't compare to null.");
        }
        ProgramVariable that = (ProgramVariable) pOther;

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
