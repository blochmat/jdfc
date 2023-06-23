package graphs.esg;

import com.google.common.collect.Sets;
import data.ClassExecutionData;
import data.DomainVariable;
import data.MethodData;
import graphs.esg.nodes.ESGNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class ESGCreator {

    private static int ESG_NODE_INDEX;

    public static void createESGsForClass(ClassExecutionData cData) {
        for(MethodData mData : cData.getMethods().values()) {
            ESG_NODE_INDEX = 0;
            mData.setEsg(ESGCreator.createESGForMethod(cData, mData));
            mData.getSg().calculateReachingDefinitions();
            mData.calculateInterDefUsePairs();
        }
    }

    public static ESGImpl createESGForMethod(ClassExecutionData cData, MethodData mData) {
//        SGImpl sg = mData.getSg();
//        Set<DomainVariable> domain = mData.getSg().getDomain();
//        Map<DomainVariable, DomainVariable> domainVarMap = mData.getSg().getDomainVarMap();
//        Map<Integer, ESGNode> nodes = Maps.newTreeMap();
//        Multimap<Integer, Integer> edges = ArrayListMultimap.create();
//
//        ESGNode zero = new ESGNode(new DomainVariable.ZeroVariable());
//        nodes.put(ESG_NODE_INDEX, zero);
//
//        Set<DomainVariable> initialDVars = domain.stream()
//                .filter(var ->
//                        Objects.equals(var.getMethodName(), mData.buildInternalMethodName())).collect(Collectors.toSet());
//
//        for(SGNode sgNode : sg.getNodes().values()) {
//            for(DomainVariable initial : initialDVars) {
//                DomainVariable dVar = initial;
//                while(Objects.equals(dVar.getClassName(), sgNode.get) )
//                if (sgNode instanceof SGCallNode) {
//                   // replace dVar by matched var
//                } else {
//
//                }
//                nodes.put(ESG_NODE_INDEX, new ESGNode(dVar));
//                ESG_NODE_INDEX++;
//            }
//        }

        return null;
    }

    public static Set<ESGNode> createNodes(Set<DomainVariable> dVarSet) {
        Set<ESGNode> nodes = Sets.newLinkedHashSet();
        for(DomainVariable dVar : dVarSet) {
            nodes.add(new ESGNode(dVar));
        }
        return nodes;
    }


}
