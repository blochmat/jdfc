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

    @JsonIgnore
    private CompilationUnit srcFileAst;

    @JsonIgnore
    private PackageDeclaration pkgDecl;

    @JsonIgnore
    private List<ImportDeclaration> impDeclList;

    @JsonIgnore
    private ClassOrInterfaceDeclaration ciDecl;

    private String relativePath;

    private Map<String, String> nestedTypeMap;

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
        this.fields = new HashSet<>();
        this.methods = extractMethodDeclarations(this.ciDecl);
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
            throw new IllegalArgumentException(String.format("Class \"%s\" is not present in file \"%s\".", cName, this.relativePath));
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
        logger.debug("Return NULL");
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

    public void computeCoverage() {
        logger.debug(String.format("%s.computeCoverageForClass", this.getName()));
        for (MethodData mData : this.getMethods().values()) {
            logger.debug(mData.buildInternalMethodName());
            String internalMethodName = mData.buildInternalMethodName();
            if (mData.getPairs().size() == 0) {
                continue;
            }
            logger.debug("Pairs present.");
            for (DefUsePair pair : mData.getPairs()) {
                ProgramVariable def = pair.getDefinition();
                ProgramVariable use = pair.getUsage();

                if (def.isCovered() && use.isCovered()) {
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
}
