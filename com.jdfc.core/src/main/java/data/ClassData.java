package data;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import data.singleton.CoverageDataStore;
import graphs.cfg.LocalVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;
import utils.JavaParserHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coverage data container of a single class. It contains information about all methods including CFG's, Def-Use pairs,
 * inter-procedural matches, covered and uncovered variables.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ClassData extends ExecutionData implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient CompilationUnit srcFileAst;

    private transient PackageDeclaration pkgDecl;

    private transient List<ImportDeclaration> impDeclList;

    private transient ClassOrInterfaceDeclaration ciDecl;

    private UUID id;

    private String relativePath;

    private Map<String, String> nestedTypeMap;

    private Map<UUID, MethodData> methods;

    private Map<Integer, UUID> lineToMethodIdMap;

    private Map<UUID, Map<UUID, ProgramVariable>> fieldDefinitions;

    public ClassData(String fqn, String name, UUID id, String pRelativePath, CompilationUnit srcFileAst) {
        super(fqn, name);
        this.id = id;
        this.relativePath = pRelativePath;
        this.lineToMethodIdMap = new HashMap<>();
        this.fieldDefinitions = new HashMap<>();
        this.srcFileAst = srcFileAst;
        this.pkgDecl = extractPackageDeclaration(srcFileAst);
        this.impDeclList = extractImportDeclarationList(srcFileAst);
        this.ciDecl = extractClassDeclaration(srcFileAst, name);
        this.nestedTypeMap = extractNestedTypes(srcFileAst);
        this.methods = extractMethodDeclarations(this.ciDecl);
    }

    public String toString() {
        return String.format("ParentFqn: %s Fqn: %s RelPath: %s Methods: %d Total: %d Covered: %d Rate: %f", getParentFqn(), getFqn(), relativePath, getMethodCount(), getTotal(), getCovered(), getRate());
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

    private Map<UUID, MethodData> extractMethodDeclarations(ClassOrInterfaceDeclaration ciAst) {
        Map<UUID, MethodData> methods = new HashMap<>();
        for(MethodDeclaration mDecl : ciAst.getMethods()) {
            JavaParserHelper javaParserHelper = new JavaParserHelper();
            // jvm patter built from JavaParser: (I)LBuilder; [IndexOutOfBoundsException]
            String jvmTypeDesc = javaParserHelper.toJvmTypeDescriptor(mDecl);
            String jvmExcDesc = javaParserHelper.toJvmExcDescriptor(mDecl);

            // Return and param types
            Set<Type> types = new HashSet<>();
            types.add(mDecl.getType());
            types.addAll(mDecl.getParameters().stream().map(Parameter::getType).collect(Collectors.toSet()));
            Set<ResolvedType> resolvedTypes = types.stream().map(Type::resolve).collect(Collectors.toSet());

            // Exception types
            Set<Type> excTypes = mDecl.getThrownExceptions().stream().map(ReferenceType::asReferenceType).collect(Collectors.toSet());
            Set<ResolvedType> resolvedExcTypes = excTypes.stream().map(Type::resolve).collect(Collectors.toSet());

            // add full relative paths: (I)Lcom/jdfc/Option$Builder; [java/lang/IndexOutOfBoundsException]
            String jvmAsmTypeDesc = javaParserHelper.buildJvmAsmTypeDesc(
                    resolvedTypes,
                    nestedTypeMap,
                    jvmTypeDesc,
                    new HashSet<>(),
                    new HashSet<>()
            );

            String jvmAsmExcDesc = javaParserHelper.buildJvmAsmExcDesc(
                    resolvedExcTypes,
                    nestedTypeMap,
                    jvmExcDesc,
                    new HashSet<>(),
                    new HashSet<>()
            );

            int mAccess = mDecl.getAccessSpecifier().ordinal();
            String mName = mDecl.getName().getIdentifier();
            String jvmAsmDesc = jvmAsmExcDesc.equals("") ? jvmAsmTypeDesc : String.format("%s %s", jvmAsmTypeDesc, jvmAsmExcDesc);

            UUID id = UUID.randomUUID();
            MethodData mData = new MethodData(id, this.relativePath, mAccess, mName, jvmAsmDesc, mDecl);

            methods.put(id, mData);
            for(int i = mData.getBeginLine(); i <= mData.getEndLine(); i++) {
                this.lineToMethodIdMap.put(i, id);
            }
            JDFCUtils.logThis(JDFCUtils.prettyPrintMap(lineToMethodIdMap), "lineToMethodIdMap");
        }

        for(ConstructorDeclaration cDecl : ciAst.getConstructors()) {
            JavaParserHelper javaParserHelper = new JavaParserHelper();
            // jvm patter built from JavaParser: (I)LBuilder; [IndexOutOfBoundsException]
            String jvmTypeDesc = javaParserHelper.toJvmTypeDescriptor(cDecl);
            String jvmExcDesc = javaParserHelper.toJvmExcDescriptor(cDecl);

            // Return and param types
            Set<Type> types = cDecl.getParameters().stream().map(Parameter::getType).collect(Collectors.toSet());
            Set<ResolvedType> resolvedTypes = types.stream().map(Type::resolve).collect(Collectors.toSet());

            // Exception types
            Set<Type> excTypes = cDecl.getThrownExceptions().stream().map(ReferenceType::asReferenceType).collect(Collectors.toSet());
            Set<ResolvedType> resolvedExcTypes = excTypes.stream().map(Type::resolve).collect(Collectors.toSet());

            // add full relative paths: (I)Lcom/jdfc/Option$Builder; [java/lang/IndexOutOfBoundsException]
            String jvmAsmTypeDesc = javaParserHelper.buildJvmAsmTypeDesc(
                    resolvedTypes,
                    nestedTypeMap,
                    jvmTypeDesc,
                    new HashSet<>(),
                    new HashSet<>()
            );

            String jvmAsmExcDesc = javaParserHelper.buildJvmAsmExcDesc(
                    resolvedExcTypes,
                    nestedTypeMap,
                    jvmExcDesc,
                    new HashSet<>(),
                    new HashSet<>()
            );

            int mAccess = cDecl.getAccessSpecifier().ordinal();
            String mName = "<init>";
            String jvmAsmDesc = jvmAsmExcDesc.equals("") ? jvmAsmTypeDesc : String.format("%s %s", jvmAsmTypeDesc, jvmAsmExcDesc);

            UUID id = UUID.randomUUID();
            MethodData mData = new MethodData(id, this.relativePath, mAccess, mName, jvmAsmDesc, cDecl);
            methods.put(id, mData);

            for(int i = mData.getBeginLine(); i <= mData.getEndLine(); i++) {
                this.lineToMethodIdMap.put(i, id);
            }
        }

        // Add default constructor
        if (methods.values().stream().noneMatch(x -> x.getName().equals("<init>"))) {
            UUID id = UUID.randomUUID();
            MethodData mData = new MethodData(id, this.relativePath, AccessSpecifier.PUBLIC.ordinal(), "<init>", "()V;");
            methods.put(id, mData);

            this.lineToMethodIdMap.put(Integer.MIN_VALUE, id);
        }

        JDFCUtils.logThis(JDFCUtils.prettyPrintMap(lineToMethodIdMap), "lineToMethodIdMap");

        return methods;
    }

    public MethodData getMethodByInternalName(String internalName) {
        for(MethodData mData : methods.values()) {
            if (mData.buildInternalMethodName().equals(internalName)) {
                return mData;
            }
        }

        if(log.isDebugEnabled()) {
            File transformFile = JDFCUtils.createFileInDebugDir("getMethodByInternalName.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                writer.write(String.format("Search param: %s", internalName));
                writer.write(JDFCUtils.prettyPrintArray(
                        methods.values().stream().map(MethodData::buildInternalMethodName).toArray()));
                writer.write("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();

            }
        }
        return null;
    }

    public MethodData getMethodByShortInternalName(String internalName) {
        for(MethodData mData : methods.values()) {
            if (mData.buildInternalMethodName().contains(internalName)) {
                return mData;
            }
        }

        if(log.isDebugEnabled()) {
            File transformFile = JDFCUtils.createFileInDebugDir("getMethodByShortInternalName.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                writer.write(String.format("Search param: %s", internalName));
                writer.write(JDFCUtils.prettyPrintArray(
                        methods.values().stream().map(MethodData::buildInternalMethodName).toArray()));
                writer.write("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();

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

    public void computeCoverage() {
        for(MethodData mData : this.getMethods().values()) {
            String internalMethodName = mData.buildInternalMethodName();
            if (mData.getPairs().size() == 0) {
                continue;
            }

            for (DefUsePair pair : mData.getPairs().values()) {
                ProgramVariable def = CoverageDataStore.getInstance().getProgramVariableMap().get(pair.getDefId());
                ProgramVariable use = CoverageDataStore.getInstance().getProgramVariableMap().get(pair.getUseId());

                if (def.getIsCovered() && use.getIsCovered()) {
                    if (!internalMethodName.contains("<clinit>")) {
                        this.getMethodByInternalName(internalMethodName).findDefUsePair(pair).setCovered(true);
                    }
                } else {
                    if (!internalMethodName.contains("<clinit>")) {
                        this.getMethodByInternalName(internalMethodName).findDefUsePair(pair).setCovered(false);
                    }
                }
                if (!internalMethodName.contains("<clinit>")) {
                    mData.computeCoverageMetadata();
                }
            }

        }
        this.calculateMethodCount();
        this.calculateTotal();
        this.calculateCovered();
        this.calculateRate();
    }


    public void calculateMethodCount() {
        this.setMethodCount(this.methods.size());
    }

    public void calculateTotal() {
        this.setTotal(methods.values().stream().mapToInt(MethodData::getTotal).sum());
    }

    public void calculateCovered() {
        this.setCovered(methods.values().stream().mapToInt(MethodData::getCovered).sum());
    }

    public void calculateRate() {
        if (getTotal() != 0.0) {
            this.setRate((double) getCovered() / getTotal());
        } else {
            this.setRate(0.0);
        }
    }

    public LocalVariable findLocalVariable(final String internalMethodName,
                                           final int pVarIndex) {
        // TODO
        if(!internalMethodName.contains("<clinit>")) {
            Map<Integer, LocalVariable> localVariableTable = this.getMethodByInternalName(internalMethodName)
                    .getLocalVariableTable();
            return localVariableTable.get(pVarIndex);
        } else {
            return null;
        }
    }
}
