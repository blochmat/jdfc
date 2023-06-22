package graphs.esg;

import data.ClassExecutionData;
import data.DomainVariable;
import data.MethodData;
import graphs.sg.SG;
import graphs.sg.nodes.SGNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public class ESGCreator {

    public static void createESGsForClass(ClassExecutionData cData) {
        for(MethodData mData : cData.getMethods().values()) {
            mData.setEsg(ESGCreator.createESGForMethod(cData, mData));
            mData.getSg().calculateReachingDefinitions();
            mData.calculateInterDefUsePairs();
        }
    }

    public static ESG createESGForMethod(ClassExecutionData cData, MethodData mData) {
        SG sg = mData.getSg();
        Set<DomainVariable> domain = mData.getSg().getDomain();
        Map<DomainVariable, DomainVariable> domainVarMap = mData.getSg().getDomainVarMap();

        for(SGNode node : sg.getNodes().values()) {
            // TODO: For every node create as many nodes as domain vars are present.
            //       Connect all nodes corresponding to the same domain var.
            //       If a new method is reached: use the domain vars of this method an connect them according to the mapping
            //       One node needs: index, domainvar, some boolean, pred, succ,
            //       One edge needs: index -> [index] mapping

        }

        return null;
    }
}
