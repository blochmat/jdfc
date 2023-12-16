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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PairData implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String className;
    private String methodName;
    private UUID defId;
    private UUID useId;
    private boolean isCovered;

    public PairData(UUID id,
                    String className,
                    String methodName,
                    UUID defId,
                    UUID useId) {
        this.id = id;
        this.className = className;
        this.methodName = methodName;
        this.defId = defId;
        this.useId = useId;
    }

    public ProgramVariable getDefFromStore() {
        return ProjectData.getInstance().getProgramVariableMap().get(defId);
    }

    public ProgramVariable getUseFromStore() {
        return ProjectData.getInstance().getProgramVariableMap().get(useId);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairData pair = (PairData) o;
        return Objects.equals(getDefId(), pair.getDefId()) && Objects.equals(getUseId(), pair.getUseId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(defId, useId);
    }

    // Serialization
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
        writeString(out, className);
        writeString(out, methodName);
        out.writeLong(defId.getMostSignificantBits());
        out.writeLong(defId.getLeastSignificantBits());
        out.writeLong(useId.getMostSignificantBits());
        out.writeLong(useId.getLeastSignificantBits());
        out.writeBoolean(isCovered);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        long mostSigBits = in.readLong();
        long leastSigBits = in.readLong();
        id = new UUID(mostSigBits, leastSigBits);
        className = readString(in);
        methodName = readString(in);
        mostSigBits = in.readLong();
        leastSigBits = in.readLong();
        defId = new UUID(mostSigBits, leastSigBits);
        mostSigBits = in.readLong();
        leastSigBits = in.readLong();
        useId = new UUID(mostSigBits, leastSigBits);
        isCovered = in.readBoolean();
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
