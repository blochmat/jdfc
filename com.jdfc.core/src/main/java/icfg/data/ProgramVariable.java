package icfg.data;

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
    private final boolean isDefinition;

    private ProgramVariable(final String pOwner,
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

    public static ProgramVariable create(final String pOwner,
                                         final String pName,
                                         final String pDescriptor,
                                         final int pInstructionIndex,
                                         final int pLineNumber,
                                         final boolean pIsDefinition) {
        return new ProgramVariable(pOwner, pName, pDescriptor, pInstructionIndex, pLineNumber, pIsDefinition);
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
        return String.format("ProgramVariable %s: %s (Idx: %d, LNr: %d, Owner: %s, isDefinition: %s)",
                name, descriptor, instructionIndex, lineNumber, owner, isDefinition);
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
