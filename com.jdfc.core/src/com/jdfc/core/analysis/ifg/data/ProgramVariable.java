package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

/**
 * Represents a program variable that is identified by its name and type.
 */
public class ProgramVariable implements Comparable<Object> {

    private final String owner;
    private final String name;
    private final String descriptor;
    private final String method;
    private final int instructionIndex;
    private final int lineNumber;
    private boolean isReference;
    private boolean isDefinition;

    private ProgramVariable(final String pOwner,
                            final String pName,
                            final String pDescriptor,
                            final String pMethod,
                            final int pInstructionIndex,
                            final int pLineNumber,
                            final boolean pIsReference,
                            final boolean pIsDefinition) {
        owner = pOwner;
        name = pName;
        descriptor = pDescriptor;
        method = pMethod;
        instructionIndex = pInstructionIndex;
        lineNumber = pLineNumber;
        isReference = pIsReference;
        isDefinition = pIsDefinition;
    }

    public static ProgramVariable create(final String pOwner,
                                         final String pName,
                                         final String pDescriptor,
                                         final String pMethod,
                                         final int pInstructionIndex,
                                         final int pLineNumber,
                                         final boolean pIsReference,
                                         final boolean pIsDefinition) {
        return new ProgramVariable(pOwner, pName, pDescriptor, pMethod, pInstructionIndex, pLineNumber, pIsReference, pIsDefinition);
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

    public String getMethod() {
        return method;
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
                && Objects.equals(method, that.method)
                && (instructionIndex == that.instructionIndex)
                && (lineNumber == that.lineNumber)
                && Boolean.compare(isReference, that.isReference) == 0
                && Boolean.compare(isDefinition, that.isDefinition) == 0;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, instructionIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ProgramVariable %s: %s (Idx: %d, LNr: %d, Owner: %s, Method: %s isReference: %s, isDefinition: %s)",
                name, descriptor, instructionIndex, lineNumber, owner, method, isReference, isDefinition);
    }

    public static String encode(ProgramVariable pVar) {
        if(pVar != null) {
            return String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                    pVar.owner, pVar.name, pVar.descriptor, pVar.method,
                    pVar.instructionIndex, pVar.lineNumber, pVar.isReference, pVar.isDefinition);
        } else {
            return "null";
        }
    }

    public static ProgramVariable decode(String pEncoded) {
        if(pEncoded.equals("null")) {
            return null;
        }
        String[] props = pEncoded.split("\\|");
        String owner = (props[0].equals("null") ? null : props[0]);
        String name = (props[1].equals("null") ? null : props[1]);
        String descriptor = (props[2].equals("null") ? null : props[2]);
        String pMethod = (props[3].equals("null") ? null : props[3]);
        int instructionIndex = Integer.parseInt(props[4]);
        int lineNumber = Integer.parseInt(props[5]);
        boolean isReference = Boolean.parseBoolean(props[6]);
        boolean isDefinition = Boolean.parseBoolean(props[7]);

        return ProgramVariable.create(owner, name, descriptor, pMethod, instructionIndex, lineNumber, isReference, isDefinition);
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
