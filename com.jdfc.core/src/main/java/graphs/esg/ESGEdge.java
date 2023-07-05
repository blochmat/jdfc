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
    private String sourceMethodId;

    /**
     * Name of the target node's method
     */
    private String targetMethodId;

    /**
     * Source Domain Variable Index
     */
    private ProgramVariable sourceVar;

    /**
     * Target Domain Variable Index
     */
    private ProgramVariable targetVar;

    /**
     *
     * @param sgnSourceIdx Super Graph Source Node Index
     * @param sgnTargetIdx Super Graph Target Node Index
     * @param sourceVar Source Domain Variable Index
     * @param targetVar Target Domain Variable Index
     */
    public ESGEdge(final int sgnSourceIdx,
                   final int sgnTargetIdx,
                   final String sourceMethodId,
                   final String targetMethodId,
                   final ProgramVariable sourceVar,
                   final ProgramVariable targetVar) {
        this.sgnSourceIdx = sgnSourceIdx;
        this.sgnTargetIdx = sgnTargetIdx;
        this.sourceMethodId = sourceMethodId;
        this.targetMethodId = targetMethodId;
        this.sourceVar = sourceVar;
        this.targetVar = targetVar;
    }

    @Override
    public String toString() {
        String sourceVarStr;
        if(sourceVar.getInstructionIndex() != Integer.MIN_VALUE) {
            sourceVarStr = String.format("%s:%s", sourceVar.getName(), sourceVar.getInstructionIndex());
        } else {
            sourceVarStr = sourceVar.getName();
        }

        String targetVarStr;
        if(targetVar.getInstructionIndex() != Integer.MIN_VALUE) {
            targetVarStr = String.format("%s:%s", targetVar.getName(), targetVar.getInstructionIndex());
        } else {
            targetVarStr = targetVar.getName();
        }

        return "ESGEdge{"
                + sgnSourceIdx + ", "
                + sgnTargetIdx + ", "
                + sourceMethodId + ", "
                + targetMethodId + ", "
                + sourceVarStr + ", "
                + targetVarStr + '}';
    }
}
