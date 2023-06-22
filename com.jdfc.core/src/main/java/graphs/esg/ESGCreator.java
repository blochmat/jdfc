package graphs.esg;

import data.ClassExecutionData;
import data.DomainVariable;
import data.MethodData;
import graphs.sg.SG;
import graphs.sg.nodes.SGNode;
import lombok.extern.slf4j.Slf4j;

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
        Set<DomainVariable> domain = mData.getCfg().getDomain();
//        mData.getSg().get

        for(SGNode node : sg.getNodes().values()) {

        }

        return null;
    }
}
