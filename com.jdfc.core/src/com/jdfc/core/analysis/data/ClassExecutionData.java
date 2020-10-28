package com.jdfc.core.analysis.data;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.Pair;
import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.ifg.*;
import com.jdfc.core.analysis.ifg.data.DefUsePair;
import com.jdfc.core.analysis.ifg.data.InstanceVariable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassExecutionData extends ExecutionData {

    private Map<String, CFG> methodCFGs;
    private final Map<String, Integer> methodFirstLine;
    private final Set<InstanceVariable> instanceVariables;
    private final Set<InstanceVariable> instanceVariablesOccurrences;
    private final TreeMap<String, List<DefUsePair>> defUsePairs;
    private final Map<String, Set<ProgramVariable>> defUseCovered;
    private final Map<String, Set<ProgramVariable>> defUseUncovered;

    //TODO Make parameterMatching a map
    private final Set<Pair<ProgramVariable, ProgramVariable>> parameterMatching;
    private final String relativePath;

    // TODO Initialize methodCFGs here
    public ClassExecutionData(String pRelativePath) {
        methodFirstLine = new HashMap<>();
        defUsePairs = new TreeMap<>();
        defUseCovered = new HashMap<>();
        defUseUncovered = new HashMap<>();
        relativePath = pRelativePath;
        instanceVariables = new HashSet<>();
        parameterMatching = new HashSet<>();
        instanceVariablesOccurrences = new HashSet<>();
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

    public Map<String, Integer> getMethodFirstLine() {
        return methodFirstLine;
    }

    public TreeMap<String, List<DefUsePair>> getDefUsePairs() {
        return defUsePairs;
    }

    public Map<String, Set<ProgramVariable>> getDefUseCovered() {
        return defUseCovered;
    }

    public Set<InstanceVariable> getInstanceVariables() {
        return instanceVariables;
    }

    public Set<InstanceVariable> getInstanceVariablesOccurrences() {
        return instanceVariablesOccurrences;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public Set<Pair<ProgramVariable, ProgramVariable>> getParameterMatching() {
        return parameterMatching;
    }

    public void setupMethodEntries() {
        for(Map.Entry<String, CFG> entry : methodCFGs.entrySet()){
            defUsePairs.put(entry.getKey(), new ArrayList<>());
            defUseCovered.put(entry.getKey(), new HashSet<>());
        }
    }

    /**
     * Calculates all possible Def-Use-Pairs.
     */
    public void setupIntraProceduralDefUseInformation() {
        for (Map.Entry<String, CFG> entry : methodCFGs.entrySet()) {
            for (CFGNode node : entry.getValue().getNodes().values()) {
                for (ProgramVariable def : node.getReach()) {
                    for (ProgramVariable use : node.getUses()) {
                        if (def.getName().equals(use.getName()) && !def.getType().equals("UNKNOWN")) {
                            defUsePairs.get(entry.getKey()).add(new DefUsePair(def, use));
                            if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                                defUseCovered.get(entry.getKey()).add(def);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setupInterProceduralDefUseInformation() {
        for (Map.Entry<String, CFG> methodCFGs : methodCFGs.entrySet()) {
            String methodName = methodCFGs.getKey();
            CFG graph = methodCFGs.getValue();
            for (Map.Entry<Integer, CFGNode> node : graph.getNodes().entrySet()) {
                if (node.getValue() instanceof IFGNode) {
                    IFGNode ifgNode = (IFGNode) node.getValue();
                    CFGNode entryNode = ifgNode.getCallNode();
                    String entryMethodName = ifgNode.getMethodNameDesc();
                    Map<ProgramVariable, ProgramVariable> usageDefinitionMatch =
                            getUsageDefinitionMatchRecursive(
                                    ifgNode.getParameterCount() - 1, null, ifgNode, entryNode);
                    matchPairs(methodName, entryMethodName, usageDefinitionMatch);

                    if(ifgNode.getRelatedCFG().isImpure()) {
                        insertNewDefinitions(ifgNode, usageDefinitionMatch.keySet());
                    }
                }
            }
        }
    }

    public void insertNewDefinitions(IFGNode pNode, Collection<ProgramVariable> pMethodParameters) {
        for(ProgramVariable parameter : pMethodParameters) {
            if(!isSimpleType(parameter.getType())) {
                ProgramVariable newParamDefinition =
                        ProgramVariable.create(parameter.getOwner(), parameter.getName(), parameter.getType(),
                                pNode.getIndex(), pNode.getLineNumber());
                pNode.addDefinition(newParamDefinition);
            }
        }

        ProgramVariable caller = pNode.getMethodCaller();

        ProgramVariable newCallerDefinition =
                ProgramVariable.create(caller.getOwner(), caller.getName(), caller.getType(),
                        pNode.getIndex(), pNode.getLineNumber());
        pNode.addDefinition(newCallerDefinition);
    }

    private Map<ProgramVariable, ProgramVariable> getUsageDefinitionMatchRecursive(final int pLoopsLeft,
                                                                                   final CFGNode pNode,
                                                                                   final IFGNode pCallingNode,
                                                                                   final CFGNode pEntryNode) {
        Map<ProgramVariable, ProgramVariable> matchMap = new HashMap<>();
        // for each parameter: process one node (predecessor of pNode)
        if (pLoopsLeft >= 0) {
            CFGNode pred;
            if (pNode != null) {
                pred = (CFGNode) pNode.getPredecessors().toArray()[0];
            } else {
                pred = (CFGNode) pCallingNode.getPredecessors().toArray()[0];
            }

            // find use in procedure A
            ProgramVariable usageA = (ProgramVariable) pred.getUses().toArray()[0];
            // find correlated definition in procedure B
            ProgramVariable definitionB = (ProgramVariable) pEntryNode.getDefinitions().toArray()[pLoopsLeft];

            matchMap.put(usageA, definitionB);

            matchMap = mergeMaps(matchMap,
                    getUsageDefinitionMatchRecursive(
                            pLoopsLeft - 1, pred, pCallingNode, pEntryNode));

        }
        return matchMap;
    }

    private void matchPairs(final String pMethodName,
                            final String pEntryMethodName,
                            final Map<ProgramVariable, ProgramVariable> pUsageDefinitionMatch) {
        for(Map.Entry<ProgramVariable, ProgramVariable> usageDefinitionMatch : pUsageDefinitionMatch.entrySet()) {
            ProgramVariable usageA = usageDefinitionMatch.getKey();
            ProgramVariable definitionB = usageDefinitionMatch.getValue();
            // find definition by use of procedure A
            ProgramVariable definitionA = findDefinitionByUse(pMethodName, usageA);
            if (definitionA != null) {
                // match definitions of procedure A and B
                parameterMatching.add(new Pair<>(definitionA, definitionB));
                // find all usages of definition of procedure B
                List<ProgramVariable> usagesB = findUsagesByDefinition(pEntryMethodName, definitionB);
                // add new pairs
                for (ProgramVariable usageB : usagesB) {
                    defUsePairs.get(pMethodName).add(new DefUsePair(definitionA, usageB));
                }
            }
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

    public void computeCoverageForClass() {
        this.calculateMethodCount();
        this.calculateTotal();
        int covered = 0;
        for (Map.Entry<String, List<DefUsePair>> entry : defUsePairs.entrySet()) {
            if (entry.getValue().size() == 0) {
                continue;
            }
            String methodName = entry.getKey();
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
        }
        this.setCovered(covered);
    }

    public int computeCoveredForMethod(String pKey) {
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

    public void calculateTotal() {
        this.setTotal(defUsePairs.values().stream().mapToInt(List::size).sum());
    }

    public void calculateMethodCount() {
        this.setMethodCount((int) defUsePairs.entrySet().stream().filter(x -> x.getValue().size() != 0).count());
    }

    private Map<ProgramVariable, ProgramVariable> mergeMaps(Map<ProgramVariable, ProgramVariable> map1,
                                                                    Map<ProgramVariable, ProgramVariable> map2) {
        return Stream.of(map1, map2)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private boolean isSimpleType(final String pDescriptor){
        return pDescriptor.equals("I")
                || pDescriptor.equals("D")
                || pDescriptor.equals("F")
                || pDescriptor.equals("L")
                || pDescriptor.equals("Ljava/lang/String;");
    }
}
