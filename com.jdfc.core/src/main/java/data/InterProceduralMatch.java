package data;

import icfg.data.ProgramVariable;

public class InterProceduralMatch {

    final ProgramVariable definition;
    final ProgramVariable callSiteDefinition;
    final String methodName;
    final String callSiteMethodName;

    private InterProceduralMatch(final ProgramVariable pDefinition,
                                 final ProgramVariable pCallSiteDefinition,
                                 final String pMethodName,
                                 final String pCallSiteMethodName) {
        definition = pDefinition;
        callSiteDefinition = pCallSiteDefinition;
        methodName = pMethodName;
        callSiteMethodName = pCallSiteMethodName;
    }

    public static InterProceduralMatch create(final ProgramVariable pDefinition,
                                              final ProgramVariable pCallSiteDefinition,
                                              final String pMethodName,
                                              final String pCallSiteMethodName) {
        return new InterProceduralMatch(pDefinition, pCallSiteDefinition, pMethodName, pCallSiteMethodName);
    }

    public ProgramVariable getDefinition() {
        return definition;
    }

    public ProgramVariable getCallSiteDefinition() {
        return callSiteDefinition;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getCallSiteMethodName() {
        return callSiteMethodName;
    }
}
