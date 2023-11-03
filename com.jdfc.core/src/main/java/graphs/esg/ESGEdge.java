package graphs.esg;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class ESGEdge {

    private int srcIdx;
    private int trgtIdx;
    private int srcCallSeqIdx;
    private int trgtCallSeqIdx;
    private UUID srcVarId;
    private UUID trgtVarId;

    /**
     *
     * @param srcIdx Super Graph Source Node Index
     * @param trgtIdx Super Graph Target Node Index
     * @param srcVarId Source Domain Variable Index
     * @param trgtVarId Target Domain Variable Index
     */
    public ESGEdge(final int srcIdx,
                   final int trgtIdx,
                   final int srcCallSeqIdx,
                   final int trgtCallSeqIdx,
                   final UUID srcVarId,
                   final UUID trgtVarId) {
        this.srcIdx = srcIdx;
        this.trgtIdx = trgtIdx;
        this.srcCallSeqIdx = srcCallSeqIdx;
        this.trgtCallSeqIdx = trgtCallSeqIdx;
        this.srcVarId = srcVarId;
        this.trgtVarId = trgtVarId;
    }

    @Override
    public String toString() {
        return "ESGEdge{"
                + srcIdx + ", "
                + trgtIdx + ", "
                + srcCallSeqIdx + ", "
                + trgtCallSeqIdx + ", "
                + srcVarId + ", "
                + trgtVarId + '}';
    }
}
