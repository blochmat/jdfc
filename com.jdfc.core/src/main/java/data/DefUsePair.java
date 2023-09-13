package data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefUsePair implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String className;
    private String methodName;
    private UUID defId;
    private UUID useId;
    private boolean isCovered;

    public DefUsePair(UUID id,
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
        DefUsePair pair = (DefUsePair) o;
        return isCovered() == pair.isCovered()  && Objects.equals(getDefId(), pair.getDefId()) && Objects.equals(getUseId(), pair.getUseId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(defId, useId);
    }
}
