package graphs.esg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.ClassExecutionData;
import data.DomainVariable;
import data.MethodData;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.SGNode;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.util.*;
import java.util.stream.Collectors;

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

    public static ESG createESGForMethod(ClassExecutionData cData, MethodData mData) {
        SG sg = mData.getSg();
        Set<DomainVariable> domain = mData.getSg().getDomain();
        DomainVariable zero = new DomainVariable.ZeroVariable();

        Map<Integer, ESGNode> nodes = Maps.newTreeMap();
        Map<DomainVariable, DomainVariable> domainVarMap = mData.getSg().getDomainVarMap();
        Set<DomainVariable> initialDVars = domain.stream()
                .filter(var ->
                        Objects.equals(var.getMethodName(), mData.buildInternalMethodName())).collect(Collectors.toSet());
        initialDVars.add(zero);

        for(Map.Entry<Integer, SGNode> sgNodeEntry : sg.getNodes().entrySet()) {
            int idx = sgNodeEntry.getKey();
            SGNode sgNode = sgNodeEntry.getValue();
            for(DomainVariable initial : initialDVars) {
                DomainVariable dVar = initial;
                while(!(Objects.equals(dVar, zero) || Objects.equals(dVar.getClassName(), sgNode.getClassName()))) {
                    dVar = domainVarMap.get(dVar);
                }

                nodes.put(ESG_NODE_INDEX, new ESGNode(dVar));
                ESG_NODE_INDEX++;
            }

            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < initialDVars.size(); i++) {
                    int esgIdx = idx * initialDVars.size() + i;
                    if (i == initialDVars.size() -1) {
                        sb.append(nodes.get(esgIdx).getVar().getName()).append("\n");
                    } else {
                        sb.append(nodes.get(esgIdx).getVar().getName()).append(" ");
                    }
                }

                JDFCUtils.logThis(sb.toString(), "exploded");
            }
        }

        Multimap<Integer, Integer> edges = ArrayListMultimap.create();
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
