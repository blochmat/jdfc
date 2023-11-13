package graphs.esg;

import com.google.common.collect.Multimap;
import data.ProgramVariable;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESG {
    private SG sg;
    private NavigableMap<Integer, ESGNode> nodes;
    private Multimap<Integer, ESGEdge> edges;
    private Map<String, Map<UUID, ProgramVariable>> domain;
    private NavigableMap<Integer, Map<ProgramVariable, ProgramVariable>> callerToCalleeDefinitionMap;
    private NavigableMap<Integer, Map<ProgramVariable, ProgramVariable>> calleeToCallerDefinitionMap;
}
