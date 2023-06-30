package graphs.esg;

import data.ProgramVariable;
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
    private ProgramVariable sourcePVar;

    /**
     * Target Domain Variable Index
     */
    private ProgramVariable targetPVar;

    /**
     *
     * @param sgnSourceIdx Super Graph Source Node Index
     * @param sgnTargetIdx Super Graph Target Node Index
     * @param sourcePVar Source Domain Variable Index
     * @param targetPVar Target Domain Variable Index
     */
    public ESGEdge(final int sgnSourceIdx,
                   final int sgnTargetIdx,
                   final String sourceDVarMethodName,
                   final String targetDVarMethodName,
                   final ProgramVariable sourcePVar,
                   final ProgramVariable targetPVar) {
        this.sgnSourceIdx = sgnSourceIdx;
        this.sgnTargetIdx = sgnTargetIdx;
        this.sourceDVarMethodName = sourceDVarMethodName;
        this.targetDVarMethodName = targetDVarMethodName;
        this.sourcePVar = sourcePVar;
        this.targetPVar = targetPVar;
    }

    @Override
    public String toString() {
        String sourceVarStr;
        if(sourcePVar.getInstructionIndex() != Integer.MIN_VALUE) {
            sourceVarStr = String.format("%s:%s", sourcePVar.getName(), sourcePVar.getInstructionIndex());
        } else {
            sourceVarStr = sourcePVar.getName();
        }

        String targetVarStr;
        if(targetPVar.getInstructionIndex() != Integer.MIN_VALUE) {
            targetVarStr = String.format("%s:%s", targetPVar.getName(), targetPVar.getInstructionIndex());
        } else {
            targetVarStr = targetPVar.getName();
        }

        return "ESGEdge{"
                + sgnSourceIdx + ", "
                + sgnTargetIdx + ", "
                + sourceDVarMethodName + ", "
                + targetDVarMethodName + ", "
                + sourceVarStr + ", "
                + targetVarStr + '}';
    }
}
