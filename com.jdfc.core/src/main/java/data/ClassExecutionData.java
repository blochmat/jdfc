package data;

import cfg.data.LocalVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;
import utils.JavaParserHelper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Coverage data container of a single class. It contains information about all methods including CFG's, Def-Use pairs,
 * inter-procedural matches, covered and uncovered variables.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ClassExecutionData extends ExecutionData {

    @JsonIgnore
    private Logger logger = LoggerFactory.getLogger(ClassExecutionData.class);
    private String relativePath;
    @JsonIgnore
    private CompilationUnit srcFileAst;
    @JsonIgnore
    private PackageDeclaration pkgDecl;
    @JsonIgnore
    private List<ImportDeclaration> impDeclList;
    @JsonIgnore
    private ClassOrInterfaceDeclaration ciDecl;
    private Map<String, String> nestedTypeMap;
    @JsonIgnore
    private Map<String, Integer> methodFirstLine;
    @JsonIgnore
    private Map<String, Integer> methodLastLine;
    @JsonIgnore
    private TreeMap<String, List<DefUsePair>> defUsePairs;
    @JsonIgnore
    private TreeMap<String, Map<DefUsePair, Boolean>> defUsePairsCovered;
    @JsonIgnore
    private Map<String, Set<ProgramVariable>> variablesCovered;
    @JsonIgnore
    private Map<String, Set<ProgramVariable>> variablesUncovered;
    @JsonIgnore
    private Set<InterProceduralMatch> interProceduralMatches;
    private Set<ProgramVariable> fields;
    private Map<Integer, MethodData> methods;

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
            JavaParserHelper javaParserHelper = new JavaParserHelper();
            Set<Type> types = new HashSet<>();
            // Add return, param and exception types
            types.add(mDecl.getType());
            types.addAll(mDecl.getParameters().stream().map(Parameter::getType).collect(Collectors.toSet()));
            types.addAll(mDecl.getThrownExceptions().stream().map(ReferenceType::asReferenceType).collect(Collectors.toSet()));
            // jvm patter built from JavaParser: (I)LBuilder; [IndexOutOfBoundsException]
            String jvmDesc = javaParserHelper.toJvmDescriptor(mDecl);
            // add full relative paths: (I)Lcom/jdfc/Option$Builder; [java/lang/IndexOutOfBoundsException]
            Set<ResolvedType> resolvedTypes = types.stream().map(Type::resolve).collect(Collectors.toSet());
            String jvmAsmDesc = javaParserHelper.buildJvmAsmDesc(resolvedTypes, nestedTypeMap, jvmDesc, new HashSet<>(),
                    new HashSet<>());

            int mAccess = mDecl.getAccessSpecifier().ordinal();
            String mName = mDecl.getName().getIdentifier();

            MethodData mData = new MethodData(mAccess, mName, jvmAsmDesc, mDecl);
            methods.put(mData.getBeginLine(), mData);
        }

        return methods;
    }

    // New Code
    public MethodData getMethodByInternalName(String internalName) {
        logger.debug(String.format("getMethodByInternalName(%s)", internalName));
        for(MethodData mData : methods.values()) {
            if (mData.buildInternalMethodName().equals(internalName)) {
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

    public void computeCoverageForClass() {
        logger.debug(String.format("%s.computeCoverageForClass", this.getName()));
        for (MethodData mData : this.getMethods().values()) {
            logger.debug(mData.buildInternalMethodName());
            String internalMethodName = mData.buildInternalMethodName();
            if (mData.getPairs().size() == 0) {
                continue;
            }
            logger.debug("Pairs present.");
            logger.debug(mData.getCoveredVars().toString());
            for (DefUsePair pair : mData.getPairs()) {
                ProgramVariable def = pair.getDefinition();
                ProgramVariable use = pair.getUsage();
                boolean isDefCovered;
                boolean isUseCovered;
                if(mData.getCoveredVars() != null) {
                    isDefCovered = mData.getCoveredVars().contains(def);
                    isUseCovered = mData.getCoveredVars().contains(use);
                } else {
                    isDefCovered = false;
                    isUseCovered = false;
                }

                logger.debug(String.format("isDefCov: %b, isUseCov: %b", isDefCovered, isUseCovered));

                if (isDefCovered && isUseCovered) {
                    if (!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
                        this.getMethodByInternalName(internalMethodName).findDefUsePair(pair).setCovered(true);
                        logger.debug("COVERED");
                    } else {
                        // TODO: "<init>: ()V" is not in methods
                    }
                } else {
                    if (!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
                        this.getMethodByInternalName(internalMethodName).findDefUsePair(pair).setCovered(false);
                        logger.debug("NOT COVERED");
                    } else {
                        // TODO: "<init>: ()V" is not in methods
                    }
                    if (!isDefCovered) {
                        logger.debug(String.format("Uncovered: %s", def));
                        mData.getUncoveredVars().add(def);
                    }
                    if (!isUseCovered) {
                        logger.debug(String.format("Uncovered: %s", use));
                        mData.getUncoveredVars().add(use);
                    }
                }
                if (!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
                    mData.computeCoverage();
                } else {
                    // TODO: "<init>: ()V" is not in methods
                }
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


    public void calculateMethodCount() {
        logger.debug("calculateMethodCount");
        this.setMethodCount(this.methods.size());
    }

    public void calculateTotal() {
        logger.debug("calculateTotal");
        this.setTotal(methods.values().stream().mapToInt(MethodData::getTotal).sum());
    }

    public void calculateCovered() {
        logger.debug("calculateCovered");
        this.setCovered(methods.values().stream().mapToInt(MethodData::getCovered).sum());
    }

    public void calculateRate() {
        logger.debug("calculateRate");
        if (getTotal() != 0.0) {
            this.setRate((double) getCovered() / getTotal());
        } else {
            this.setRate(0.0);
        }
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

    public LocalVariable findLocalVariable(final String internalMethodName,
                                           final int pVarIndex) {
        // TODO
        if(!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
            Map<Integer, LocalVariable> localVariableTable = this.getMethodByInternalName(internalMethodName)
                    .getLocalVariableTable();
            return localVariableTable.get(pVarIndex);
        } else {
            return null;
        }
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
}
