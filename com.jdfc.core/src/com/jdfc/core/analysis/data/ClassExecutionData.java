package com.jdfc.core.analysis.data;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.core.analysis.ifg.*;
import com.jdfc.core.analysis.ifg.data.*;
import org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Coverage data container of a single class. It contains information about all methods including CFG's, Def-Use pairs,
 * inter-procedural matches, covered and uncovered variables.
 */

public class ClassExecutionData extends ExecutionData {

    private Map<String, CFG> methodCFGs;
    private final Map<String, Integer> methodFirstLine;
    private final Map<String, Integer> methodLastLine;
    private final Set<Field> fields;
    private final TreeSet<InstanceVariable> instanceVariables;
    private final TreeMap<String, List<DefUsePair>> defUsePairs;
    private final TreeMap<String, Map<DefUsePair, Boolean>> defUsePairsCovered;
    private final Map<String, Set<ProgramVariable>> variablesCovered;
    private final Map<String, Set<ProgramVariable>> variablesUncovered;
    private final Set<InterProceduralMatch> interProceduralMatches;
    private final String relativePath;

    // TODO Initialize methodCFGs here
    public ClassExecutionData(String pRelativePath) {
        methodLastLine = new HashMap<>();
        methodFirstLine = new HashMap<>();
        defUsePairs = new TreeMap<>();
        defUsePairsCovered = new TreeMap<>();
        variablesCovered = new HashMap<>();
        variablesUncovered = new HashMap<>();
        relativePath = pRelativePath;
        fields = new HashSet<>();
        interProceduralMatches = new HashSet<>();
        instanceVariables = new TreeSet<>();
    }

    /**
     * Sets the method {@link CFG}s.
     *
     * @param pMethodCFGs The mapping of method names and {@link CFG}s
     */
    public void setMethodCFGs(final Map<String, CFG> pMethodCFGs) {
        methodCFGs = pMethodCFGs;
    }

    public Map<String, Integer> getMethodFirstLine() {
        return methodFirstLine;
    }

    public Map<String, Integer> getMethodLastLine() {
        return methodLastLine;
    }

    public TreeMap<String, List<DefUsePair>> getDefUsePairs() {
        return defUsePairs;
    }

    public TreeMap<String, Map<DefUsePair, Boolean>> getDefUsePairsCovered() {
        return defUsePairsCovered;
    }

    public Map<String, Set<ProgramVariable>> getVariablesCovered() {
        return variablesCovered;
    }

    public Map<String, Set<ProgramVariable>> getVariablesUncovered() {
        return variablesUncovered;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public Set<InstanceVariable> getInstanceVariables() {
        return instanceVariables;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public Set<InterProceduralMatch> getInterProceduralMatches() {
        return interProceduralMatches;
    }

    public void initializeDefUseLists() {
        for (Map.Entry<String, CFG> entry : methodCFGs.entrySet()) {
            defUsePairs.put(entry.getKey(), new ArrayList<>());
            defUsePairsCovered.put(entry.getKey(), new HashMap<>());
            variablesCovered.put(entry.getKey(), new HashSet<>());
        }
    }

    public void insertAdditionalDefs() {
        for (Map.Entry<String, CFG> methodCFGsEntry : methodCFGs.entrySet()) {
            for (Map.Entry<Integer, CFGNode> entry : methodCFGsEntry.getValue().getNodes().entrySet()) {
                CFGNode node = entry.getValue();
                if (node instanceof IFGNode) {
                    IFGNode callingNode = (IFGNode) node;
                    if (callingNode.getRelatedCFG() != null && callingNode.getRelatedCFG().isImpure()) {
                        Map<ProgramVariable, ProgramVariable> usageDefinitionMatch =
                                getUsageDefinitionMatchRecursive(
                                        callingNode.getParameterCount(), null, callingNode, callingNode.getRelatedCallSiteNode());
                        if (usageDefinitionMatch != null && !usageDefinitionMatch.isEmpty()) {
                            insertNewDefinitions(callingNode, usageDefinitionMatch.keySet());
                        }
                    }
                }
            }
        }
    }

    public void calculateReachingDefs() {
        for (Map.Entry<String, CFG> methodCFGsEntry : methodCFGs.entrySet()) {
            methodCFGsEntry.getValue().calculateReachingDefinitions();
        }
    }

    /**
     * Calculates all possible Def-Use-Pairs.
     */
    public void calculateIntraProceduralDefUsePairs() {
        for (Map.Entry<String, CFG> methodCFGsEntry : methodCFGs.entrySet()) {
            for (Map.Entry<Integer, CFGNode> entry : methodCFGsEntry.getValue().getNodes().entrySet()) {
                CFGNode node = entry.getValue();
                for (ProgramVariable def : node.getReach()) {
                    for (ProgramVariable use : node.getUses()) {
                        if (def.getName().equals(use.getName()) && !def.getDescriptor().equals("UNKNOWN")) {
                            defUsePairs.get(methodCFGsEntry.getKey()).add(new DefUsePair(def, use));
                            if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                                variablesCovered.get(methodCFGsEntry.getKey()).add(def);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setupInterProceduralMatches() {
        for (Map.Entry<String, CFG> methodCFGs : methodCFGs.entrySet()) {
            String methodName = methodCFGs.getKey();
            CFG graph = methodCFGs.getValue();
            for (Map.Entry<Integer, CFGNode> node : graph.getNodes().entrySet()) {
                if (node.getValue() instanceof IFGNode) {
                    IFGNode ifgNode = (IFGNode) node.getValue();
                    if (ifgNode.getRelatedCFG() != null) {
                        CFGNode entryNode = ifgNode.getRelatedCallSiteNode();
                        String entryMethodName = ifgNode.getMethodNameDesc();
                        Map<ProgramVariable, ProgramVariable> usageDefinitionMatch =
                                getUsageDefinitionMatchRecursive(
                                        ifgNode.getParameterCount(), null, ifgNode, entryNode);
                        matchPairs(methodName, entryMethodName, usageDefinitionMatch);
                    }
                }
            }
        }
    }

    public void insertNewDefinitions(IFGNode pNode, Collection<ProgramVariable> pMethodParameters) {
        for (ProgramVariable parameter : pMethodParameters) {
            if (!isSimpleType(parameter.getDescriptor())) {
                ProgramVariable newParamDefinition =
                        ProgramVariable.create(parameter.getOwner(), parameter.getName(), parameter.getDescriptor(),
                                pNode.getIndex(), pNode.getLineNumber(),
                                parameter.isDefinition());
                pNode.addDefinition(newParamDefinition);
            }
        }
    }

    private Map<ProgramVariable, ProgramVariable> getUsageDefinitionMatchRecursive(final int pParameterCount,
                                                                                   final CFGNode pNode,
                                                                                   final IFGNode pCallingNode,
                                                                                   final CFGNode pRelatedCallSiteNode) {
        Map<ProgramVariable, ProgramVariable> matchMap = new HashMap<>();
        // for each parameter: process one node (predecessor of pNode)
        if (pParameterCount > 0) {
            CFGNode predecessor;

            // Example: 2 parameters
            // LOAD x
            // LOAD y
            // INVOKE  x y
            if (pNode == null) {
                predecessor = (CFGNode) pCallingNode.getPredecessors().toArray()[0];
            } else {
                predecessor = (CFGNode) pNode.getPredecessors().toArray()[0];
            }

            if(pRelatedCallSiteNode == null) {
                return matchMap;
            }

            Set<ProgramVariable> usages = predecessor.getUses();
            for (ProgramVariable var : usages) {
                // find correlated definition in procedure B
                ProgramVariable definitionB;
                // If a constructor is called with constant values
                if(pCallingNode.getMethodNameDesc().contains("<init>") && pRelatedCallSiteNode.getDefinitions().size() == 1) {
                    definitionB = (ProgramVariable) pRelatedCallSiteNode.getDefinitions().toArray()[0];
                    matchMap.put(var, definitionB);
                    return matchMap;
                }

                if (pCallingNode.getOpcode() == Opcodes.INVOKESTATIC) {
                    // Special Case: If static method or constructor is called "this" is not defined
                    definitionB = (ProgramVariable) pRelatedCallSiteNode.getDefinitions().toArray()[pParameterCount - 1];
                } else {
                    definitionB = (ProgramVariable) pRelatedCallSiteNode.getDefinitions().toArray()[pParameterCount];
                }
                matchMap.put(var, definitionB);
            }

            matchMap = mergeMaps(matchMap,
                    getUsageDefinitionMatchRecursive(
                            pParameterCount - 1, predecessor, pCallingNode, pRelatedCallSiteNode));
        }
        return matchMap;
    }

    private void matchPairs(final String pMethodName,
                            final String pEntryMethodName,
                            final Map<ProgramVariable, ProgramVariable> pUsageDefinitionMatch) {
        for (Map.Entry<ProgramVariable, ProgramVariable> usageDefinitionMatch : pUsageDefinitionMatch.entrySet()) {
            ProgramVariable usageA = usageDefinitionMatch.getKey();
            ProgramVariable definitionB = usageDefinitionMatch.getValue();
            // find definition by use of procedure A
            ProgramVariable definitionA = findDefinitionByUse(pMethodName, usageA);
            if (definitionA != null) {
                // match definitions of procedure A and B
                InterProceduralMatch newMatch = InterProceduralMatch.create(definitionA, definitionB, pMethodName, pEntryMethodName);
                interProceduralMatches.add(newMatch);
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
        for (Map.Entry<String, List<DefUsePair>> entry : defUsePairs.entrySet()) {
            String methodName = entry.getKey();
            variablesUncovered.put(methodName, new HashSet<>());
            if (entry.getValue().size() == 0) {
                continue;
            }
            for (DefUsePair pair : entry.getValue()) {
                ProgramVariable def = pair.getDefinition();
                ProgramVariable use = pair.getUsage();

                boolean isDefCovered = variablesCovered.get(methodName).contains(def);
                boolean isUseCovered = variablesCovered.get(methodName).contains(use);

                Set<InterProceduralMatch> interProceduralMatches = findInterProceduralMatches(def, methodName);
                if (!interProceduralMatches.isEmpty() && !isUseCovered) {
                    isUseCovered = checkInterProceduralUseCoverage(interProceduralMatches, use);
                }

                if (isDefCovered && isUseCovered) {
                    defUsePairsCovered.get(methodName).put(pair, true);
                } else {
                    defUsePairsCovered.get(methodName).put(pair, false);
                    if (!isDefCovered) {
                        variablesUncovered.get(methodName).add(def);
                    }
                    if (!isUseCovered) {
                        variablesUncovered.get(methodName).add(use);
                    }
                }
            }

            for (Map.Entry<DefUsePair, Boolean> element : defUsePairsCovered.get(methodName).entrySet()) {
                for (Map.Entry<DefUsePair, Boolean> compare : defUsePairsCovered.get(methodName).entrySet()) {
                    boolean isDefNameEqual = element.getKey().getDefinition().getName().equals(compare.getKey().getDefinition().getName());
                    boolean isUseEqual = element.getKey().getUsage().equals(compare.getKey().getUsage());
                    boolean isElementDefIndexSmaller = element.getKey().getDefinition().getInstructionIndex() < compare.getKey().getDefinition().getInstructionIndex();
                    boolean isElementUseIndexBiggerThanCompareDefIndex = element.getKey().getUsage().getInstructionIndex() > compare.getKey().getDefinition().getInstructionIndex();
                    boolean isCompareCovered = compare.getValue();
                    if (isDefNameEqual && isUseEqual
                            && isElementDefIndexSmaller
                            && isElementUseIndexBiggerThanCompareDefIndex
                            && isCompareCovered) {
                        element.setValue(false);
                        break;
                    }
                }
            }
        }
        this.calculateMethodCount();
        this.calculateTotal();
        this.calculateCovered();
    }

    public int computeCoveredForMethod(String pKey) {
        return (int) defUsePairsCovered.get(pKey).values().stream().filter(x -> x).count();
    }

    public void calculateMethodCount() {
        this.setMethodCount((int) defUsePairs.entrySet().stream().filter(x -> x.getValue().size() != 0).count());
    }

    public void calculateTotal() {
        this.setTotal(defUsePairs.values().stream().mapToInt(List::size).sum());
    }

    public void calculateCovered() {
        this.setCovered(defUsePairsCovered.keySet().stream().mapToInt(this::computeCoveredForMethod).sum());
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

    private Set<InterProceduralMatch> findInterProceduralMatches(final ProgramVariable pDefinition,
                                                                 final String pMethodName) {
        Set<InterProceduralMatch> result = new HashSet<>();
        for (InterProceduralMatch element : interProceduralMatches) {
            if (element.getDefinition().equals(pDefinition) && element.methodName.equals(pMethodName)) {
                result.add(element);
            }
        }

        return result;
    }

    public LocalVariable findLocalVariable(final String pMethodName,
                                           final int pVarIndex) {
        CFG cfg = methodCFGs.get(pMethodName);
        Map<Integer, LocalVariable> localVariableTable = cfg.getLocalVariableTable();
        return localVariableTable.get(pVarIndex);
    }

    public InstanceVariable findInstanceVariable(final ProgramVariable pProgramVariable) {
        for (InstanceVariable variable : this.instanceVariables) {
            if (variable.getOwner().equals(pProgramVariable.getOwner())
                    && variable.getName().equals(pProgramVariable.getName())
                    && variable.getDescriptor().equals(pProgramVariable.getDescriptor())
                    && variable.getInstructionIndex() == pProgramVariable.getInstructionIndex()
                    && variable.getLineNumber() == pProgramVariable.getLineNumber()
                    && Boolean.compare(variable.isDefinition(), pProgramVariable.isDefinition()) == 0) {
                return variable;
            }
        }
        return null;
    }


    private boolean checkInterProceduralUseCoverage(final Set<InterProceduralMatch> pInterProceduralMatches,
                                                    final ProgramVariable pUsage) {
        for (InterProceduralMatch element : pInterProceduralMatches) {
            String callSiteMethodName = element.getCallSiteMethodName();
            // TODO: B0001 - Only null because cfg creation for class fails
            if(variablesCovered.get(callSiteMethodName) != null) {
                for (ProgramVariable variable : variablesCovered.get(callSiteMethodName)) {
                    if (variable.equals(pUsage)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSimpleType(final String pDescriptor) {
        return pDescriptor.equals("I")
                || pDescriptor.equals("D")
                || pDescriptor.equals("F")
                || pDescriptor.equals("L")
                || pDescriptor.equals("Ljava/lang/String;");
    }

    public boolean isAnalyzedVariable(String pName, int pLineNumber) {
        for (Map<DefUsePair, Boolean> entryMap : defUsePairsCovered.values()) {
            for (DefUsePair element : entryMap.keySet()) {
                if ((element.getDefinition().getName().equals(pName) && element.getDefinition().getLineNumber() == pLineNumber)
                        || element.getUsage().getName().equals(pName) && element.getUsage().getLineNumber() == pLineNumber) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getMethodNameFromLineNumber(final int pLineNumber) {
        for (Map.Entry<String, Integer> firstLineEntry : methodFirstLine.entrySet()) {
            if (firstLineEntry.getValue() <= pLineNumber) {
                if (methodLastLine.get(firstLineEntry.getKey()) >= pLineNumber) {
                    return firstLineEntry.getKey();
                }
            }
        }
        return null;
    }
}
