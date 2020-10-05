package com.jdfc.core.analysis.data;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.ifg.*;

import java.util.*;
import java.util.stream.Collectors;

public class ClassExecutionData extends ExecutionData {

    private Map<String, CFG> methodCFGs;
    private Set<InstanceVariable> instanceVariables;
    private TreeMap<String, List<DefUsePair>> defUsePairs;
    private Map<String, Set<ProgramVariable>> defUseCovered;
    private Map<String, Set<ProgramVariable>> defUseUncovered;
    private Map<String, Pair<Integer, Integer>> methodRangeMap;
    private Set<Pair<ProgramVariable, ProgramVariable>> parameterMatching;
    private final String relativePath;

    public ClassExecutionData(String pRelativePath) {
        defUsePairs = new TreeMap<>();
        defUseCovered = new HashMap<>();
        defUseUncovered = new HashMap<>();
        relativePath = pRelativePath;
        instanceVariables = new HashSet<>();
        methodRangeMap = new HashMap<>();
        parameterMatching = new HashSet<>();
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

    public Map<String, Pair<Integer, Integer>> getMethodRangeMap() {
        return methodRangeMap;
    }

    public Set<InstanceVariable> getInstanceVariables() {
        return instanceVariables;
    }

    public void setInstanceVariables(Set<InstanceVariable> instanceVariables) {
        this.instanceVariables = instanceVariables;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setDefUsePairs(TreeMap<String, List<DefUsePair>> defUsePairs) {
        this.defUsePairs = defUsePairs;
    }

    public void setDefUseCovered(Map<String, Set<ProgramVariable>> defUseCovered) {
        this.defUseCovered = defUseCovered;
    }

    public Set<Pair<ProgramVariable, ProgramVariable>> getParameterMatching() {
        return parameterMatching;
    }

    /**
     * Calculates all possible Def-Use-Pairs.
     */
    public void calculateIntraproceduralDefUsePairs() {
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

    public void calculateInterproceduralDefUsePairs() {
        for (Map.Entry<String, CFG> methodCFGs : methodCFGs.entrySet()) {
            String methodName = methodCFGs.getKey();
            CFG graph = methodCFGs.getValue();
            for (Map.Entry<Integer, CFGNode> node : graph.getNodes().entrySet()) {
                if (node.getValue() instanceof IFGNode) {
                    IFGNode ifgNode = (IFGNode) node.getValue();
                    CFGNode entryNode = ifgNode.getCallNode();
                    String entryMethodName = ifgNode.getMethodNameDesc();
                    processPredRecursive(methodName, entryMethodName, ifgNode.getParameterCount() - 1, ifgNode, ifgNode, entryNode);
                    System.out.println(defUsePairs);
                }
            }
        }
    }

    // TODO: Check for refactoring
    private void processPredRecursive(String pMethodName, String pEntryMethodName, int pLoopsLeft, CFGNode pNode, IFGNode pCallingNode, CFGNode pEntryNode) {
        if (pLoopsLeft >= 0) {
            CFGNode pred = (CFGNode) pNode.getPredecessors().toArray()[0];
            ProgramVariable use = (ProgramVariable) pred.getUses().toArray()[0];
            ProgramVariable entryDefinition = (ProgramVariable) pEntryNode.getDefinitions().toArray()[pLoopsLeft];

            ProgramVariable definition = findDefinitionByUse(pMethodName, use);

            if (definition != null) {
                parameterMatching.add(new Pair<>(definition, entryDefinition));
                List<ProgramVariable> usages = findUsagesByDefinition(pEntryMethodName, entryDefinition);
                for (ProgramVariable usage : usages) {
                    defUsePairs.get(pMethodName).add(new DefUsePair(definition, usage));
                }

//                if (!(definition.getType().equals("I")
//                        || definition.getType().equals("F")
//                        || definition.getType().equals("D")
//                        || definition.getType().equals("L"))
//                        && methodCFGs.get(pEntryMethodName).isImpure()) {
//                    ProgramVariable newDefinition = ProgramVariable.create(definition.getOwner(),
//                            definition.getName(),
//                            definition.getType(),
//                            use.getInstructionIndex(),
//                            use.getLineNumber());
//                    for(DefUsePair defUsePair : defUsePairs.get(pMethodName)) {
//                        if(defUsePair.getDefinition().equals(definition)
//                                && defUsePair.getUsage().getLineNumber() > use.getLineNumber()) {
//                            defUsePair.setDefinition(newDefinition);
//                        }
//                    }
//                }
            }
            processPredRecursive(pMethodName, pEntryMethodName, pLoopsLeft - 1, pred, pCallingNode, pEntryNode);
        }

    }

    private ProgramVariable findDefinitionByUse(String pMethodName, ProgramVariable pUsage) {
        for (DefUsePair pair : defUsePairs.get(pMethodName)) {
            if (pair.getUsage().equals(pUsage)) {
                return pair.getDefinition();
            }
        }
        return null;
    }

    private List<ProgramVariable> findUsagesByDefinition(String pMethodName, ProgramVariable pDefinition) {
        List<ProgramVariable> result = new ArrayList<>();
        for (DefUsePair pair : defUsePairs.get(pMethodName)) {
            if (pair.getDefinition().equals(pDefinition)) {
                result.add(pair.getUsage());
            }
        }
        return result;
    }

    public void computeCoverage() {
        defUseUncovered = new HashMap<>();
        for (Map.Entry<String, List<DefUsePair>> entry : defUsePairs.entrySet()) {
            if (entry.getValue().size() == 0) {
                continue;
            }
            String methodName = entry.getKey();
            int covered = 0;
            defUseUncovered.put(methodName, new HashSet<>());
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
            }
            this.setCovered(covered);
            this.setMissed(this.getTotal() - this.getCovered());
        }
    }

    public int computeCoverageForMethod(String pKey) {
        List<DefUsePair> list = defUsePairs.get(pKey);
        Set<ProgramVariable> set = defUseCovered.get(pKey);

        int covered = 0;

        for (DefUsePair pair : list) {
            if (set.contains(pair.getDefinition())
                    && set.contains(pair.getUsage())) {
                covered += 1;
            }
        }
        return covered;
    }
}
