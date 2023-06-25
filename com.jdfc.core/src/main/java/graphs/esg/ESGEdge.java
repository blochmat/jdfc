package graphs.esg;

import lombok.Data;
import lombok.NoArgsConstructor;

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
    private int sourceDVarIdx;

    /**
     * Target Domain Variable Index
     */
    private int targetDVarIdx;

    /**
     *
     * @param sgnSourceIdx Super Graph Source Node Index
     * @param sgnTargetIdx Super Graph Target Node Index
     * @param sourceDVarIdx Source Domain Variable Index
     * @param targetDVarIdx Target Domain Variable Index
     */
    public ESGEdge(final int sgnSourceIdx,
                   final int sgnTargetIdx,
                   final String sourceDVarMethodName,
                   final String targetDVarMethodName,
                   final int sourceDVarIdx,
                   final int targetDVarIdx) {
        this.sgnSourceIdx = sgnSourceIdx;
        this.sgnTargetIdx = sgnTargetIdx;
        this.sourceDVarMethodName = sourceDVarMethodName;
        this.targetDVarMethodName = targetDVarMethodName;
        this.sourceDVarIdx = sourceDVarIdx;
        this.targetDVarIdx = targetDVarIdx;

    }

    @Override
    public String toString() {
        return "ESGEdge{" + sgnSourceIdx + ", " + sourceDVarMethodName + ", " + sourceDVarIdx + ", " + sgnTargetIdx + ", " + targetDVarMethodName + ", " + targetDVarIdx + '}';
    }
}
