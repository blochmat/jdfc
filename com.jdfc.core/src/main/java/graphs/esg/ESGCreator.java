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

import java.util.Map;
import java.util.Objects;
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

    public static ESG createESGForMethod(ClassExecutionData cData, MethodData mData) {
        SG sg = mData.getSg();
        DomainVariable zero = new DomainVariable.ZeroVariable();

        Map<Integer, ESGNode> nodes = Maps.newTreeMap();
        Map<String, Map<DomainVariable, DomainVariable>> domainVarMap = mData.getSg().getDomainVarMap();

        Map<Integer, DomainVariable> initialDomain = Maps.newTreeMap();
        initialDomain.put(0, zero);
        for(DomainVariable d : mData.getCfg().getDomain()) {
            initialDomain.put(d.getIndex() + 1, d);
        }

        for(SGNode sgNode : sg.getNodes().values()) {
            for(DomainVariable  dVar : initialDomain.values()) {
                JDFCUtils.logThis(dVar.toString(), "ESGCreator_methodNames");
                JDFCUtils.logThis(String.valueOf(sgNode.getIndex()), "ESGCreator_methodNames");
                JDFCUtils.logThis(String.format("%s %s", dVar.getMethodName(), sgNode.getMethodName()), "ESGCreator_methodNames");

                while(!Objects.equals(dVar, zero)
                        && domainVarMap.containsKey(sgNode.getMethodName())
                        && domainVarMap.get(sgNode.getMethodName()).containsKey(dVar)) {
                    dVar = domainVarMap.get(sgNode.getMethodName()).get(dVar);
                    JDFCUtils.logThis("Transfer " + dVar.toString(), "ESGCreator_methodNames");
                }
                nodes.put(ESG_NODE_INDEX, new ESGNode(dVar));
                ESG_NODE_INDEX++;
            }
        }


        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(cData.getRelativePath()).append(" ");
            sb.append(mData.buildInternalMethodName()).append("\n");
            for(Map.Entry<Integer, SGNode> sgNodeEntry : sg.getNodes().entrySet()) {
                int idx = sgNodeEntry.getKey();
                for (int i = 0; i < initialDomain.size(); i++) {
                    int esgIdx = idx * initialDomain.size() + i;
                    sb.append(nodes.get(esgIdx).getVar().getName()).append(" ");
                }
                sb.append("\n");
            }
            JDFCUtils.logThis(sb.toString(), "exploded");
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
