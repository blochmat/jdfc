package graphs.esg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.ClassExecutionData;
import data.DomainVariable;
import data.MethodData;
import data.ProgramVariable;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.SGNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        Map<DomainVariable, DomainVariable> domainVarMap = mData.getSg().getDomainVarMap();
        Map<Integer, ESGNode> nodes = Maps.newTreeMap();
        Multimap<Integer, Integer> edges = ArrayListMultimap.create();

        ESGNode zero = new ESGNode(new DomainVariable.ZeroVariable());
        nodes.put(ESG_NODE_INDEX, zero);

        for(SGNode sgNode : mData.getSg().getNodes().values()) {
            Set<ProgramVariable> definitions = sgNode.getDefinitions();

        }

        Set<DomainVariable> initialDVars = domain.stream()
                .filter(var ->
                        Objects.equals(var.getMethod(), mData.buildInternalMethodName())).collect(Collectors.toSet());



        for(SGNode node : sg.getNodes().values()) {
            // TODO: For every node create as many nodes as domain vars are present.
            //       Connect all nodes corresponding to the same domain var.
            //       If a new method is reached: use the domain vars of this method an connect them according to the mapping
            //       One node needs: index, domainvar, some boolean, pred, succ,
            //       One edge needs: index -> [index] mapping

        }

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
