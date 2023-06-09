package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefUsePair {

    private UUID defID;
    private UUID useID;
    private boolean covered;

    public DefUsePair(UUID defID, UUID useID) {
        this.defID = defID;
        this.useID = useID;
        this.covered = false;
    }
}
