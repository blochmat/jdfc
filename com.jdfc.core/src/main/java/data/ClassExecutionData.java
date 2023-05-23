package data;

import icfg.CFG;
import icfg.nodes.ICFGNode;
import icfg.IFGNode;
import icfg.data.DefUsePair;
import icfg.data.LocalVariable;
import icfg.data.ProgramVariable;
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
    private final TreeMap<String, List<DefUsePair>> defUsePairs;
    private final TreeMap<String, Map<DefUsePair, Boolean>> defUsePairsCovered;
    private final Map<String, Set<ProgramVariable>> variablesCovered;
    private final Map<String, Set<ProgramVariable>> variablesUncovered;
    private final Set<InterProceduralMatch> interProceduralMatches;
    private final String relativePath;

    public ClassExecutionData(String pRelativePath) {
        methodLastLine = new HashMap<>();
        methodFirstLine = new HashMap<>();
        defUsePairs = new TreeMap<>();
        defUsePairsCovered = new TreeMap<>();
        variablesCovered = new HashMap<>();
        variablesUncovered = new HashMap<>();
        relativePath = pRelativePath;
        interProceduralMatches = new HashSet<>();
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

    public String getRelativePath() {
        return relativePath;
    }

    public Set<InterProceduralMatch> getInterProceduralMatches() {
        return interProceduralMatches;
    }

    /**
     * Initalize all required lists for every method
     */
    public void initializeDefUseLists() {
        for (Map.Entry<String, CFG> entry : methodCFGs.entrySet()) {
            defUsePairs.put(entry.getKey(), new ArrayList<>());
            defUsePairsCovered.put(entry.getKey(), new HashMap<>());
            variablesCovered.put(entry.getKey(), new HashSet<>());
        }
    }

    /**
     * Inserts new defintions in case of an impure method invoke. New definitions are added for parameters passed
     * with a complex (object) type
     */
    public void insertAdditionalDefs() {
        for (Map.Entry<String, CFG> methodCFGsEntry : methodCFGs.entrySet()) {
            for (Map.Entry<Integer, ICFGNode> entry : methodCFGsEntry.getValue().getNodes().entrySet()) {
                ICFGNode node = entry.getValue();
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

    /**
     * Calulates reaching defintions for all method cfgs
     */
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
            for (Map.Entry<Integer, ICFGNode> entry : methodCFGsEntry.getValue().getNodes().entrySet()) {
                ICFGNode node = entry.getValue();
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

    /**
     * Established inter-procedural connection between parameters
     */
    public void setupInterProceduralMatches() {
        for (Map.Entry<String, CFG> methodCFGs : methodCFGs.entrySet()) {
            String methodName = methodCFGs.getKey();
            CFG graph = methodCFGs.getValue();
            for (Map.Entry<Integer, ICFGNode> node : graph.getNodes().entrySet()) {
                if (node.getValue() instanceof IFGNode) {
                    IFGNode ifgNode = (IFGNode) node.getValue();
                    if (ifgNode.getRelatedCFG() != null) {
                        ICFGNode entryNode = ifgNode.getRelatedCallSiteNode();
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

    /**
     * Insert new definitons at a {@code IFGNode} in case of an impure method invoke
     *
     * @param pNode Node invoking a method
     * @param pMethodParameters parameters involved in method call
     */
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

    /**
     * Computes the variable connections to establish inter-procedural matching of parameters
     * @param pParameterCount Count of parameters involved
     * @param pNode Invoking node n for the first iteration, then predecessor of n
     * @param pCallingNode Invoking node n
     * @param pRelatedCallSiteNode entry node of invoked procedure
     * @return Map matching definitions in both procedures to one another
     */
    private Map<ProgramVariable, ProgramVariable> getUsageDefinitionMatchRecursive(final int pParameterCount,
                                                                                   final ICFGNode pNode,
                                                                                   final IFGNode pCallingNode,
                                                                                   final ICFGNode pRelatedCallSiteNode) {
        Map<ProgramVariable, ProgramVariable> matchMap = new HashMap<>();
        // for each parameter: process one node (predecessor of pNode)
        if (pParameterCount > 0) {
            ICFGNode predecessor;

            // Example: 2 parameters
            // LOAD x
            // LOAD y
            // INVOKE  x y
            if (pNode == null) {
                predecessor = (ICFGNode) pCallingNode.getPredecessors().toArray()[0];
            } else {
                predecessor = (ICFGNode) pNode.getPredecessors().toArray()[0];
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

    /**
     * Create matching between program variables of procedures
     *
     * @param pMethodName Method name of invoking procedure
     * @param pEntryMethodName Method name of invoked procedure
     * @param pUsageDefinitionMatch Map of matching definitions
     */
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

    /**
     * Finds definition from given use
     *
     * @param pMethodName Method name of method containing definition and use
     * @param pUsage given use
     * @return definition of given use
     */
    private ProgramVariable findDefinitionByUse(String pMethodName, ProgramVariable pUsage) {
        for (DefUsePair pair : defUsePairs.get(pMethodName)) {
            if (pair.getUsage().equals(pUsage)) {
                return pair.getDefinition();
            }
        }
        return null;
    }

    /**
     * Find all usages from given defintion
     * @param pMethodName Method name of method containing definition and uses
     * @param pDefinition given definition
     * @return List of all usages associated with given definition
     */
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
                boolean isDefCovered;
                boolean isUseCovered;
                if(variablesCovered.get(methodName) != null) {
                    isDefCovered = variablesCovered.get(methodName).contains(def);
                    isUseCovered = variablesCovered.get(methodName).contains(use);
                } else {
                    isDefCovered = false;
                    isUseCovered = false;
                }

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

            // What does this?
            // If two DU-Pairs definition has the same name and the use is equal
            // and the first pairs def index is smaller
            // and the second pairs def lies between the first def and use
            // then set DU-Pair as uncovered

            // This should probably filter out redefinitions, but this does not make sense, because redefintions
            // should not be a part of def use pairs anyways
//            for (Map.Entry<DefUsePair, Boolean> element : defUsePairsCovered.get(methodName).entrySet()) {
//                for (Map.Entry<DefUsePair, Boolean> compare : defUsePairsCovered.get(methodName).entrySet()) {
//                    boolean isDefNameEqual = element.getKey().getDefinition().getName()
//                            .equals(compare.getKey().getDefinition().getName());
//                    boolean isUseEqual = element.getKey().getUsage().equals(compare.getKey().getUsage());
//                    boolean isElementDefIndexSmaller = element.getKey().getDefinition().getInstructionIndex()
//                            < compare.getKey().getDefinition().getInstructionIndex();
//                    boolean isElementUseIndexBiggerThanCompareDefIndex = element.getKey().getUsage().getInstructionIndex()
//                            > compare.getKey().getDefinition().getInstructionIndex();
//                    boolean isCompareCovered = compare.getValue();
//                    if (isDefNameEqual && isUseEqual
//                            && isElementDefIndexSmaller
//                            && isElementUseIndexBiggerThanCompareDefIndex
//                            && isCompareCovered) {
//                        if (element.getKey().getDefinition().getName().contains("result") && element.getKey().getDefinition().getLineNumber() == 8) {
//                            System.out.println("hello");
//                        }
//                        element.setValue(false);
//                        break;
//                    }
//                }
//            }
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
