package graphs.esg;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
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
     * Name of the source node's method
     */
    private String sourceDVarMethodName;

    /**
     * Name of the target node's method
     */
    private String targetDVarMethodName;

    /**
     * Source Domain Variable Index
     */
    private UUID sourcePVarId;

    /**
     * Target Domain Variable Index
     */
    private UUID targetPVarId;

    /**
     *
     * @param sgnSourceIdx Super Graph Source Node Index
     * @param sgnTargetIdx Super Graph Target Node Index
     * @param sourcePVarId Source Domain Variable Index
     * @param targetPVarId Target Domain Variable Index
     */
    public ESGEdge(final int sgnSourceIdx,
                   final int sgnTargetIdx,
                   final String sourceDVarMethodName,
                   final String targetDVarMethodName,
                   final UUID sourcePVarId,
                   final UUID targetPVarId) {
        this.sgnSourceIdx = sgnSourceIdx;
        this.sgnTargetIdx = sgnTargetIdx;
        this.sourceDVarMethodName = sourceDVarMethodName;
        this.targetDVarMethodName = targetDVarMethodName;
        this.sourcePVarId = sourcePVarId;
        this.targetPVarId = targetPVarId;

    }

    @Override
    public String toString() {
        return "ESGEdge{" + sgnSourceIdx + ", " + sourceDVarMethodName + ", " + sourcePVarId + ", " + sgnTargetIdx + ", " + targetDVarMethodName + ", " + targetPVarId + '}';
    }
}
