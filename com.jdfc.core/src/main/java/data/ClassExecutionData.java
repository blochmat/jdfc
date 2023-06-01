package data;

import cfg.CFG;
import cfg.data.LocalVariable;
import cfg.nodes.CFGNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;
import utils.JavaPaserHelper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Coverage data container of a single class. It contains information about all methods including CFG's, Def-Use pairs,
 * inter-procedural matches, covered and uncovered variables.
 */

public class ClassExecutionData extends ExecutionData {

    private final Logger logger = LoggerFactory.getLogger(ClassExecutionData.class);
    private final String relativePath;
    private final CompilationUnit srcFileAst;
    private final PackageDeclaration pkgDecl;
    private final List<ImportDeclaration> impDeclList;
    private final ClassOrInterfaceDeclaration ciDecl;
//    private final List<ClassOrInterfaceDeclaration> innerCiDeclList;
    private final Map<String, String> nestedTypeMap;
    private Map<String, CFG> methodCFGs;
    private final Map<String, Integer> methodFirstLine;
    private final Map<String, Integer> methodLastLine;
    private final TreeMap<String, List<DefUsePair>> defUsePairs;
    private final TreeMap<String, Map<DefUsePair, Boolean>> defUsePairsCovered;
    private final Map<String, Set<ProgramVariable>> variablesCovered;
    private final Map<String, Set<ProgramVariable>> variablesUncovered;
    private final Set<InterProceduralMatch> interProceduralMatches;
    private final Set<ProgramVariable> fields;
    private final Map<Integer, MethodData> methods;

    public ClassExecutionData(String fqn, String name, String pRelativePath, CompilationUnit srcFileAst) {
        super(fqn, name);
        relativePath = pRelativePath;
        this.srcFileAst = srcFileAst;
        this.pkgDecl = extractPackageDeclaration(srcFileAst);
        this.impDeclList = extractImportDeclarationList(srcFileAst);
        this.ciDecl = extractClassDeclaration(srcFileAst, name);
        this.nestedTypeMap = extractNestedTypes(srcFileAst);
        this.methods = extractMethodDeclarations(this.ciDecl);

        // TODO: Most of this stuff should go into MethodData
        methodLastLine = new HashMap<>();
        methodFirstLine = new HashMap<>();
        defUsePairs = new TreeMap<>();
        defUsePairsCovered = new TreeMap<>();
        variablesCovered = new HashMap<>();
        variablesUncovered = new HashMap<>();
        interProceduralMatches = new HashSet<>();
        fields = new HashSet<>();
    }

    public String toString() {
        return String.format("ParentFqn: %s%nFqn: %s%nRelPath: %s%nMethods: %d%nTotal: %d%nCovered: %d%nRate: %f%n", getParentFqn(), getFqn(), relativePath, getMethodCount(), getTotal(), getCovered(), getRate());
    }

    private PackageDeclaration extractPackageDeclaration(CompilationUnit cu){
        Optional<PackageDeclaration> pkdDeclOpt = cu.getPackageDeclaration();
        return pkdDeclOpt.orElse(null);
    }

    private List<ImportDeclaration> extractImportDeclarationList(CompilationUnit cu) {
        NodeList<ImportDeclaration> impDeclList = cu.getImports();
        return new ArrayList<>(impDeclList);
    }

    private ClassOrInterfaceDeclaration extractClassDeclaration(CompilationUnit srcFileAst, String name) {
        String cName = name.replace(".class", "");
        Optional<ClassOrInterfaceDeclaration> ciOptional = srcFileAst.getClassByName(cName);
        if (ciOptional.isPresent()) {
            return ciOptional.get();
        } else {
            throw new IllegalArgumentException("Class is not present in file.");
        }
    }

    /**
     * Find all nested classes, create their JVM internal representation, and save the mapping
     *
     * @param srcFileAst
     * @return
     */
    private Map<String, String> extractNestedTypes(CompilationUnit srcFileAst) {
        Map<String, String> result = new HashMap<>();
        srcFileAst.findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(ClassOrInterfaceDeclaration::isNestedType)
                .forEach(c -> {
                    String cFqn = c.resolve().getQualifiedName();
                    String jvmInternal = JDFCUtils.innerClassFqnToJVMInternal(cFqn);
                    result.put(c.getName().getIdentifier(), jvmInternal);
                });
        return result;
    }

    private Map<Integer, MethodData> extractMethodDeclarations(ClassOrInterfaceDeclaration ciAst) {
        Map<Integer, MethodData> methods = new HashMap<>();
        for(MethodDeclaration mDecl : ciAst.getMethods()) {
            JavaPaserHelper javaPaserHelper = new JavaPaserHelper();
            Set<Type> types = new HashSet<>();
            // Add return, param and exception types
            types.add(mDecl.getType());
            types.addAll(mDecl.getParameters().stream().map(Parameter::getType).collect(Collectors.toSet()));
            types.addAll(mDecl.getThrownExceptions().stream().map(ReferenceType::asReferenceType).collect(Collectors.toSet()));
            // jvm patter built from JavaParser: (I)LBuilder; [IndexOutOfBoundsException]
            String jvmDesc = javaPaserHelper.toJvmDescriptor(mDecl);
            // add full relative paths: (I)Lcom/jdfc/Option$Builder; [java/lang/IndexOutOfBoundsException]
            Set<ResolvedType> resolvedTypes = types.stream().map(Type::resolve).collect(Collectors.toSet());
            String jvmAsmDesc = javaPaserHelper.buildJvmAsmDesc(resolvedTypes, nestedTypeMap, jvmDesc);

            int mAccess = mDecl.getAccessSpecifier().ordinal();
            String mName = mDecl.getName().getIdentifier();

            MethodData mData = new MethodData(mAccess, mName, jvmAsmDesc, mDecl);
            methods.put(mData.getBeginLine(), mData);
        }

        return methods;
    }

    private String buildJvmAsmDesc(Set<ResolvedType> resolvedTypes, String jvmDesc) {
        if (jvmDesc.contains("()LList")) {
            System.out.println("FOUND");
        }

        for(ResolvedType resolvedType : resolvedTypes) {
            try {
                if (resolvedType.isReferenceType()) {
                    Optional<ResolvedReferenceTypeDeclaration> typeDeclaration = resolvedType.asReferenceType().getTypeDeclaration();
                    if (typeDeclaration.isPresent()) {
                        ResolvedReferenceTypeDeclaration rrtd = typeDeclaration.get();
                        if (rrtd.isClass()) {
                            if(this.nestedTypeMap.containsKey(rrtd.getName())) {
                                // inner or nested class
                                jvmDesc = jvmDesc.replace(rrtd.getName(), this.nestedTypeMap.get(rrtd.getName()));
                            } else {
                                // java native class
                                String newName = rrtd.getQualifiedName().replace(".", "/");
                                jvmDesc = jvmDesc.replace(rrtd.getName(), newName);
                            }
                        } else {
                            System.out.println("HEH");
                        }
                    }
                } else if (resolvedType.isArray()) {
                    ResolvedArrayType rat = resolvedType.asArrayType();
                    ResolvedType rt = rat.getComponentType();
                    resolvedTypes.add(rt);
                    resolvedTypes.remove(resolvedType);
                    jvmDesc = buildJvmAsmDesc(resolvedTypes, jvmDesc);
                }
            } catch (Exception e) {
                System.out.println("Exception");
                // ...
            }
        }
        return jvmDesc;
    }


    // New Code
    public MethodData getMethodByInternalName(String internalName) {
        System.err.println("DEBUG "+methods.size());
        for(MethodData mData : methods.values()) {
            if (mData.buildInternalMethodName().equals(internalName)) {
                return mData;
            }
        }
        return null;
    }

    public MethodData getMethodByName(String name) {
        for(MethodData mData : methods.values()) {
            if (mData.getName().equals(name)) {
                return mData;
            }
        }
        return null;
    }

    public MethodData getMethodByLineNumber(int lNr) {
        for(MethodData mData : methods.values()) {
            if (mData.getBeginLine() <= lNr && lNr <= mData.getEndLine()) {
                return mData;
            }
        }
        return null;
    }


    // TODO: Old Code

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
//    public void insertAdditionalDefs() {
//        for (Map.Entry<String, CFG> methodCFGsEntry : methodCFGs.entrySet()) {
//            for (Map.Entry<Double, CFGNode> entry : methodCFGsEntry.getValue().getNodes().entrySet()) {
//                CFGNode node = entry.getValue();
//                if (node instanceof ToBeDeleted) {
//                    ToBeDeleted callingNode = (ToBeDeleted) node;
//                    if (callingNode.getRelatedCFG() != null && callingNode.getRelatedCFG().isImpure()) {
//                        Map<ProgramVariable, ProgramVariable> usageDefinitionMatch =
//                                getUsageDefinitionMatchRecursive(
//                                        callingNode.getParameterCount(), null, callingNode, callingNode.getRelatedCallSiteNode());
//                        if (usageDefinitionMatch != null && !usageDefinitionMatch.isEmpty()) {
//                            insertNewDefinitions(callingNode, usageDefinitionMatch.keySet());
//                        }
//                    }
//                }
//            }
//        }
//    }

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
            for (Map.Entry<Double, CFGNode> entry : methodCFGsEntry.getValue().getNodes().entrySet()) {
                CFGNode node = entry.getValue();
                for (ProgramVariable def : node.getReach()) {
                    for (ProgramVariable use : node.getUses()) {
                        if (def.getName().equals(use.getName()) && !def.getDescriptor().equals("UNKNOWN")) {
                            defUsePairs.get(methodCFGsEntry.getKey()).add(new DefUsePair(def, use));
                            if (!methodCFGsEntry.getKey().contains("<init>")) {
                                this.getMethodByInternalName(methodCFGsEntry.getKey()).getPairs().add(new DefUsePair(def, use));
                            } else {
                                // TODO: "<init>: ()V" is not in methods
                            }
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
//    public void setupInterProceduralMatches() {
//        for (Map.Entry<String, CFG> methodCFGs : methodCFGs.entrySet()) {
//            String methodName = methodCFGs.getKey();
//            CFG graph = methodCFGs.getValue();
//            for (Map.Entry<Double, CFGNode> node : graph.getNodes().entrySet()) {
//                if (node.getValue() instanceof ToBeDeleted) {
//                    ToBeDeleted toBeDeleted = (ToBeDeleted) node.getValue();
//                    if (toBeDeleted.getRelatedCFG() != null) {
//                        CFGNode entryNode = toBeDeleted.getRelatedCallSiteNode();
//                        String entryMethodName = toBeDeleted.getMethodNameDesc();
//                        Map<ProgramVariable, ProgramVariable> usageDefinitionMatch =
//                                getUsageDefinitionMatchRecursive(
//                                        toBeDeleted.getParameterCount(), null, toBeDeleted, entryNode);
//                        matchPairs(methodName, entryMethodName, usageDefinitionMatch);
//                    }
//                }
//            }
//        }
//    }

    /**
     * Insert new definitons at a {@code IFGNode} in case of an impure method invoke
     *
     * @param pNode Node invoking a method
     * @param pMethodParameters parameters involved in method call
     */
//    public void insertNewDefinitions(ToBeDeleted pNode, Collection<ProgramVariable> pMethodParameters) {
//        for (ProgramVariable parameter : pMethodParameters) {
//            if (!isSimpleType(parameter.getDescriptor())) {
//                ProgramVariable newParamDefinition =
//                        ProgramVariable.create(parameter.getOwner(), parameter.getName(), parameter.getDescriptor(),
//                                pNode.getIndex(), pNode.getLineNumber(),
//                                parameter.isDefinition());
//                pNode.addDefinition(newParamDefinition);
//            }
//        }
//    }

    /**
     * Computes the variable connections to establish inter-procedural matching of parameters
     * @param pParameterCount Count of parameters involved
     * @param pNode Invoking node n for the first iteration, then predecessor of n
     * @param pCallingNode Invoking node n
     * @param pRelatedCallSiteNode entry node of invoked procedure
     * @return Map matching definitions in both procedures to one another
     */
//    private Map<ProgramVariable, ProgramVariable> getUsageDefinitionMatchRecursive(final int pParameterCount,
//                                                                                   final CFGNode pNode,
//                                                                                   final ToBeDeleted pCallingNode,
//                                                                                   final CFGNode pRelatedCallSiteNode) {
//        Map<ProgramVariable, ProgramVariable> matchMap = new HashMap<>();
//        // for each parameter: process one node (predecessor of pNode)
//        if (pParameterCount > 0) {
//            CFGNode predecessor;
//
//            // Example: 2 parameters
//            // LOAD x
//            // LOAD y
//            // INVOKE  x y
//            if (pNode == null) {
//                predecessor = (CFGNode) pCallingNode.getPredecessors().toArray()[0];
//            } else {
//                predecessor = (CFGNode) pNode.getPredecessors().toArray()[0];
//            }
//
//            if(pRelatedCallSiteNode == null) {
//                return matchMap;
//            }
//
//            Set<ProgramVariable> usages = predecessor.getUses();
//            for (ProgramVariable var : usages) {
//                // find correlated definition in procedure B
//                ProgramVariable definitionB;
//                // If a constructor is called with constant values
//                if(pCallingNode.getMethodNameDesc().contains("<init>") && pRelatedCallSiteNode.getDefinitions().size() == 1) {
//                    definitionB = (ProgramVariable) pRelatedCallSiteNode.getDefinitions().toArray()[0];
//                    matchMap.put(var, definitionB);
//                    return matchMap;
//                }
//
//                if (pCallingNode.getOpcode() == Opcodes.INVOKESTATIC) {
//                    // Special Case: If static method or constructor is called "this" is not defined
//                    definitionB = (ProgramVariable) pRelatedCallSiteNode.getDefinitions().toArray()[pParameterCount - 1];
//                } else {
//                    definitionB = (ProgramVariable) pRelatedCallSiteNode.getDefinitions().toArray()[pParameterCount];
//                }
//                matchMap.put(var, definitionB);
//            }
//
//            matchMap = mergeMaps(matchMap,
//                    getUsageDefinitionMatchRecursive(
//                            pParameterCount - 1, predecessor, pCallingNode, pRelatedCallSiteNode));
//        }
//        return matchMap;
//    }

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

                if (isDefCovered && isUseCovered) {
                    defUsePairsCovered.get(methodName).put(pair, true);
                    if (!methodName.contains("<init>")) {
                        this.getMethodByInternalName(methodName).findDefUsePair(pair).setCovered(true);
                    } else {
                        // TODO: "<init>: ()V" is not in methods
                    }
                } else {
                    defUsePairsCovered.get(methodName).put(pair, false);
                    if (!methodName.contains("<init>")) {
                        this.getMethodByInternalName(methodName).findDefUsePair(pair).setCovered(false);
                    } else {
                        // TODO: "<init>: ()V" is not in methods
                    }
                    if (!isDefCovered) {
                        variablesUncovered.get(methodName).add(def);
                    }
                    if (!isUseCovered) {
                        variablesUncovered.get(methodName).add(use);
                    }
                }
            }

            if (!methodName.contains("<init>")) {
                this.getMethodByInternalName(methodName).computeCoverage();
            } else {
                // TODO: "<init>: ()V" is not in methods
            }
        }
        this.calculateMethodCount();
        this.calculateTotal();
        this.calculateCovered();
        this.calculateRate();
    }


    public void computeCoverageForClassOld() {
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
        }
        this.calculateMethodCount();
        this.calculateTotal();
        this.calculateCovered();
        this.calculateRate();
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

    public void calculateRate() {
        this.setRate((double) getCovered() / getTotal());
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
        CFG CFG = methodCFGs.get(pMethodName);
        Map<Integer, LocalVariable> localVariableTable = CFG.getLocalVariableTable();
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


    private List<MethodDeclaration> extractMethodDeclList() {
        Optional<ClassOrInterfaceDeclaration> ciOptional = this.srcFileAst.getClassByName(getName());
        if (ciOptional.isPresent()) {
            ClassOrInterfaceDeclaration ci = ciOptional.get();
            return ci.getMethods();
        } else {
            throw new IllegalArgumentException("Class is not present in file.");
        }
    }


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

    public Set<ProgramVariable> getFields(){
        return fields;
    }

    public Map<Integer, MethodData> getMethods() {
        return methods;
    }
}
