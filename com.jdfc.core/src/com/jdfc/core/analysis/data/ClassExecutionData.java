package com.jdfc.core.analysis.data;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.core.analysis.cfg.*;

import java.util.*;
import java.util.stream.Collectors;

public class ClassExecutionData extends ExecutionData {

    private Map<String, CFG> methodCFGs;
    private Set<InstanceVariable> instanceVariables;
    private TreeMap<String, List<DefUsePair>> defUsePairs;
    private Map<String, Set<ProgramVariable>> defUseCovered;
    private Map<String, Set<ProgramVariable>> defUseUncovered;
    private Map<String, ProgramVariable> methodStartLineMap;
    private final String relativePath;

    public ClassExecutionData(String pRelativePath) {
        // TODO: Initialize all properties
        relativePath = pRelativePath;
        instanceVariables = new HashSet<>();
    }

    /**
     * Sets the method {@link CFG}s.
     *
     * @param pMethodCFGs The mapping of method names and {@link CFG}s
     */
    public void setMethodCFGs(final Map<String, CFG> pMethodCFGs) {
        methodCFGs = pMethodCFGs;
    }

    /**
     * Returns the mapping of method names and {@link CFG}s.
     *
     * @return The mapping of method names and {@link CFG}s
     */
    public Map<String, CFG> getMethodCFGs() {
        return methodCFGs;
    }

    public TreeMap<String, List<DefUsePair>> getDefUsePairs() {
        return defUsePairs;
    }

    public Map<String, Set<ProgramVariable>> getDefUseCovered() {
        return defUseCovered;
    }

    public Map<String, Set<ProgramVariable>> getDefUseUncovered() {
        return defUseUncovered;
    }

    public Map<String, ProgramVariable> getMethodStartLineMap() {
        return methodStartLineMap;
    }

    public Set<InstanceVariable> getInstanceVariables() {
        return instanceVariables;
    }

    public void setInstanceVariables(Set<InstanceVariable> instanceVariables) {
        this.instanceVariables = instanceVariables;
    }

    public String getRelativePath(){return relativePath;}

    public void setDefUsePairs(TreeMap<String, List<DefUsePair>> defUsePairs) {
        this.defUsePairs = defUsePairs;
    }

    public void setDefUseCovered(Map<String, Set<ProgramVariable>> defUseCovered) {
        this.defUseCovered = defUseCovered;
    }

    /** Calculates all possible Def-Use-Pairs. */
    public void calculateDefUsePairs() {
        defUsePairs = new TreeMap<>();
        defUseCovered = new HashMap<>();
        for (CFG graph : methodCFGs.values()) {
            String name =
                    methodCFGs
                            .entrySet()
                            .stream()
                            .filter(stringCFGEntry -> stringCFGEntry.getValue().equals(graph))
                            .collect(Collectors.toList())
                            .get(0)
                            .getKey();
            defUsePairs.put(name, new ArrayList<>());
            defUseCovered.put(name, new HashSet<>());
            for (CFGNode node : graph.getNodes().values()) {
                for (ProgramVariable def : node.getReach()) {
                    for (ProgramVariable use : node.getUses()) {
                        if (def.getName().equals(use.getName())) {
                            defUsePairs.get(name).add(new DefUsePair(def, use));
                            if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                                defUseCovered.get(name).add(def);
                            }
                        }
                    }
                }
            }
        }
    }

    public void computeCoverage() {
        defUseUncovered = new HashMap<>();
        methodStartLineMap = new HashMap<>();
        for (Map.Entry<String, List<DefUsePair>> entry : defUsePairs.entrySet()) {
            if (entry.getValue().size() == 0) {
                continue;
            }
            String methodName = entry.getKey();
            int covered = 0;
            defUseUncovered.put(methodName, new HashSet<>());
            ProgramVariable dummy = ProgramVariable.create(null, "dummy", null, Integer.MAX_VALUE, Integer.MAX_VALUE);
            methodStartLineMap.put(methodName, dummy);
            if(methodName.contains("init")) {
                for(InstanceVariable instanceVariable : instanceVariables) {
                    if(methodStartLineMap.get(methodName).getLineNumber() > instanceVariable.getLineNumber()
                            && instanceVariable.getLineNumber() != -1){
                        ProgramVariable programVariable = ProgramVariable.create(instanceVariable.getOwner(),
                                instanceVariable.getName(),
                                instanceVariable.getDescriptor(),
                                Integer.MIN_VALUE,
                                instanceVariable.getLineNumber());
                        methodStartLineMap.put(methodName, programVariable);
                    }
                }
            }
            for (DefUsePair pair : entry.getValue()) {
                ProgramVariable def = pair.getDefinition();
                ProgramVariable use = pair.getUsage();
                boolean defCovered = defUseCovered.get(methodName).contains(def);
                boolean useCovered = defUseCovered.get(methodName).contains(use);
                if (defCovered && useCovered) {
                    covered += 1;
                } else {
                    if (!defCovered) {
                        defUseUncovered.get(methodName).add(def);
                    }
                    if (!useCovered) {
                        defUseUncovered.get(methodName).add(use);
                    }
                }
                if(methodStartLineMap.get(methodName).getLineNumber() > def.getLineNumber()
                        && def.getLineNumber() != -1){
                    methodStartLineMap.put(methodName, def);
                }
            }
            this.setCovered(covered);
            this.setMissed(this.getTotal() - this.getCovered());
        }
    }

    public int computeCoverageForMethod(String pKey){
        List<DefUsePair> list = defUsePairs.get(pKey);
        Set<ProgramVariable> set = defUseCovered.get(pKey);

        int covered = 0;

        for(DefUsePair pair : list) {
            if(set.contains(pair.getDefinition())
            && set.contains(pair.getUsage())){
                covered += 1;
            }
        }
        return covered;
    }
}
