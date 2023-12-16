package data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

import static utils.Constants.ZERO_ID;

/**
 * Represents a program variable that is identified by its name and type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramVariable implements Comparable<Object>, Serializable {

    private static final long serialVersionUID = 1L;

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
        this.id = ZERO_ID;
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

    public String buildClassNodeName() {
        return this.className.replace(".", "/");
    }

    @Override
    public String toString() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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

    public boolean isNewDefOf(ProgramVariable that) {
        return !Objects.equals(getId(), that.getId())
                && !Objects.equals(getInstructionIndex(), that.getInstructionIndex())
                && !Objects.equals(getLineNumber(), that.getLineNumber())
                && getLineNumber() < that.getLineNumber()
                && Objects.equals(getLocalVarIdx(), that.getLocalVarIdx())
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getDescriptor(), that.getDescriptor())
                && Objects.equals(getIsDefinition(), that.getIsDefinition())
                && Objects.equals(getIsCovered(), that.getIsCovered())
                && Objects.equals(getIsField(), that.getIsField());
    }

    public boolean isIntraProcUseOf(ProgramVariable that) {
        return !Objects.equals(getId(), that.getId())
                && !Objects.equals(getInstructionIndex(), that.getInstructionIndex())
                && !Objects.equals(getLineNumber(), that.getLineNumber())
                && Objects.equals(getLocalVarIdx(), that.getLocalVarIdx())
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getDescriptor(), that.getDescriptor())
                && !this.getIsDefinition()
                && that.getIsDefinition()
                && Objects.equals(getIsField(), that.getIsField());
    }

    public boolean isMatchOf(UUID id) {
        return ProjectData.getInstance().getMatchesMap().get(this.id).contains(id);
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

    // Serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(localVarIdx);
        writeString(out, className);
        writeString(out, methodName);
        writeString(out, name);
        writeString(out, descriptor);
        out.writeInt(instructionIndex);
        out.writeInt(lineNumber);
        out.writeBoolean(isDefinition);
        out.writeBoolean(isCovered);
        out.writeBoolean(isField);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        localVarIdx = in.readInt();
        className = readString(in);
        methodName = readString(in);
        name = readString(in);
        descriptor = readString(in);
        instructionIndex = in.readInt();
        lineNumber = in.readInt();
        isDefinition = in.readBoolean();
        isCovered = in.readBoolean();
        isField = in.readBoolean();
    }

    private void writeString(ObjectOutputStream out, String str) throws IOException {
        byte[] bytes = str != null ? str.getBytes(StandardCharsets.UTF_8) : new byte[0];
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private String readString(ObjectInputStream in) throws IOException {
        int length = in.readInt();
        if (length == 0) return "";
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}