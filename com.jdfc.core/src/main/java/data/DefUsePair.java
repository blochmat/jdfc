package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefUsePair {

    private String type;
    private ProgramVariable definition;
    private ProgramVariable usage;
    private boolean covered;

    public DefUsePair(ProgramVariable definition, ProgramVariable usage) {
        if(definition.getDescriptor().equals(usage.getDescriptor())){
            this.type = definition.getDescriptor();
        } else {
            throw new IllegalArgumentException("Definition and Use type are not equal.");
        }
        this.definition = definition;
        this.usage = usage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefUsePair pair = (DefUsePair) o;
        return isCovered() == pair.isCovered() && Objects.equals(getType(), pair.getType()) && Objects.equals(getDefinition(), pair.getDefinition()) && Objects.equals(getUsage(), pair.getUsage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, definition, usage);
    }
}
