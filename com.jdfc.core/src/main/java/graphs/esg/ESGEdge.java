package graphs.esg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESGEdge {
    /**
     * Super Graph Source Node Index
     */
    private int sgnSourceIdx;

    /**
     * Super Graph Target Node Index
     */
    private int sgnTargetIdx;

    /**
     * Source Domain Variable Index
     */
    private int sourceDVarIdx;

    /**
     * Target Domain Variable Index
     */
    private int targetDVarIdx;
}
