package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Represents a program variable that is identified by its name and type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramVariable implements Comparable<Object> {

    private String owner;
    private String name;
    private String descriptor;
    private int instructionIndex;
    private int lineNumber;
    private boolean isDefinition;
    private boolean isCovered;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgramVariable that = (ProgramVariable) o;
        return getInstructionIndex() == that.getInstructionIndex() && getLineNumber() == that.getLineNumber() && isDefinition() == that.isDefinition() && isCovered() == that.isCovered() && Objects.equals(getOwner(), that.getOwner()) && Objects.equals(getName(), that.getName()) && Objects.equals(getDescriptor(), that.getDescriptor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwner(), getName(), getDescriptor(), getInstructionIndex(), getLineNumber(), isDefinition(), isCovered());
    }
}
