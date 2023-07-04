package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a program variable that is identified by its name and type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramVariable implements Comparable<Object> {

    private UUID id;
    private Integer localVarIdx;
    private String className;
    private String methodName;
    private String name;
    private String descriptor;
    private Integer instructionIndex;
    private Integer lineNumber;
    private Boolean isDefinition;
    private Boolean isCovered;
    private Boolean isField;

    private ProgramVariable(String className, String methodName) {
        this.id = UUID.fromString("00000000-0000-0000-0000-000000000000");
        this.localVarIdx = -1;
        this.className = className;
        this.methodName = methodName;
        this.name = "ZERO";
        this.descriptor = "UNKNOWN";
        this.instructionIndex = Integer.MIN_VALUE;
        this.lineNumber = -1;
        this.isDefinition = true;
        this.isCovered = false;
        this.isField = false;
    }

    public static class ZeroVariable extends ProgramVariable {
       public ZeroVariable(String className, String methodName) {
           super(className, methodName);
       }
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
        if (Objects.equals(this.getLineNumber(), that.getLineNumber())) {
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
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getLocalVarIdx(), that.getLocalVarIdx())
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getDescriptor(), that.getDescriptor())
                && Objects.equals(getInstructionIndex(), that.getInstructionIndex())
                && Objects.equals(getLineNumber(), that.getLineNumber())
                && Objects.equals(getIsDefinition(), that.getIsDefinition())
                && Objects.equals(getIsCovered(), that.getIsCovered())
                && Objects.equals(getIsField(), that.getIsField());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getId(),
                getLocalVarIdx(),
                getClassName(),
                getMethodName(),
                getName(),
                getDescriptor(),
                getInstructionIndex(),
                getLineNumber(),
                getIsDefinition(),
                getIsCovered(),
                getIsField());
    }
}