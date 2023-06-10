package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
