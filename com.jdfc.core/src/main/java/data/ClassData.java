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

    private String fqn;

    private String name;

    private String relativePath;

    private String fileName;

    private Map<String, String> nestedTypeMap;

    private Set<UUID> methodDataIds;

    private Map<Integer, UUID> lineToMethodIdMap;

    private Map<UUID, Map<UUID, ProgramVariable>> fieldDefinitions;

    private int total = 0;

    private int covered = 0;

    private double rate = 0.0;

    private int methodCount = 0;

    public ClassData(String fqn, String name, UUID id, String pRelativePath, CompilationUnit srcFileAst) {
        this.id = id;
        this.fqn = fqn;
        this.name = name;
        this.relativePath = pRelativePath;
        this.lineToMethodIdMap = new HashMap<>();
        this.fieldDefinitions = new HashMap<>();
        this.srcFileAst = srcFileAst;
        this.pkgDecl = extractPackageDeclaration(srcFileAst);
        this.impDeclList = extractImportDeclarationList(srcFileAst);
        this.ciDecl = extractClassDeclaration(srcFileAst, name);
        this.nestedTypeMap = extractNestedTypes(srcFileAst);
        this.methodDataIds = new HashSet<>();
        this.extractMethodDeclarations(this.ciDecl);
    }

    public Map<UUID, MethodData> getMethodDataFromStore() {
        Map<UUID, MethodData> methodDataMap = new HashMap<>();
        for (UUID id : this.methodDataIds) {
            methodDataMap.put(id, CoverageDataStore.getInstance().getMethodDataMap().get(id));
        }
        return methodDataMap;
    }

    public MethodData getMethodByInternalName(String internalName) {
        for(MethodData mData : this.getMethodDataFromStore().values()) {
            if (mData.buildInternalMethodName().equals(internalName)) {
                return mData;
            }
        }

        if(log.isDebugEnabled()) {
            File transformFile = JDFCUtils.createFileInDebugDir("getMethodByInternalName.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                writer.write(String.format("Search param: %s", internalName));
                writer.write(JDFCUtils.prettyPrintArray(
                        this.getMethodDataFromStore().values().stream().map(MethodData::buildInternalMethodName).toArray()));
                writer.write("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();

            }
        }
        return null;
    }

    public MethodData getMethodByShortInternalName(String internalName) {
        for(MethodData mData : this.getMethodDataFromStore().values()) {
            if (mData.buildInternalMethodName().contains(internalName)) {
                return mData;
            }
        }

        if(log.isDebugEnabled()) {
            File transformFile = JDFCUtils.createFileInDebugDir("getMethodByShortInternalName.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                writer.write(String.format("Search param: %s", internalName));
                writer.write(JDFCUtils.prettyPrintArray(
                        this.getMethodDataFromStore().values().stream().map(MethodData::buildInternalMethodName).toArray()));
                writer.write("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();

            }
        }
        return null;
    }

    public MethodData getMethodByLineNumber(int lNr) {
        for(MethodData mData : this.getMethodDataFromStore().values()) {
            if (mData.getBeginLine() <= lNr && lNr <= mData.getEndLine()) {
                return mData;
            }
        }
        return null;
    }

    public void computeCoverage() {
        for(MethodData mData : this.getMethodDataFromStore().values()) {
            String internalMethodName = mData.buildInternalMethodName();
            if (mData.getDuPairIds().size() == 0) {
                continue;
            }

            for (DefUsePair pair : mData.getDUPairsFromStore().values()) {
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
        this.setMethodCount(this.methodDataIds.size());
    }

    public void calculateTotal() {
        this.setTotal(this.getMethodDataFromStore().values().stream().mapToInt(MethodData::getTotal).sum());
    }

    public void calculateCovered() {
        this.setCovered(this.getMethodDataFromStore().values().stream().mapToInt(MethodData::getCovered).sum());
    }

    public void calculateRate() {
        if (getTotal() != 0.0) {
            this.setRate((double) getCovered() / getTotal());
        } else {
            this.setRate(0.0);
        }
    }

//    public LocalVariable findLocalVariable(final String internalMethodName,
//                                           final int pVarIndex) {
//        // TODO
//        if(!internalMethodName.contains("<clinit>")) {
//            Map<Integer, LocalVariable> localVariableTable = this.getMethodByInternalName(internalMethodName)
//                    .getLocalVariableTable();
//            return localVariableTable.get(pVarIndex);
//        } else {
//            return null;
//        }
//    }

    // --Private Methods------------------------------------------------------------------------------------------------
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

    @Override
    public String toString() {
        return "ClassData{" +
                "srcFileAst=" + srcFileAst +
                ", pkgDecl=" + pkgDecl +
                ", impDeclList=" + impDeclList +
                ", ciDecl=" + ciDecl +
                ", id=" + id +
                ", fqn='" + fqn + '\'' +
                ", name='" + name + '\'' +
                ", relativePath='" + relativePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", nestedTypeMap=" + nestedTypeMap +
                ", methodDataIds=" + methodDataIds +
                ", lineToMethodIdMap=" + lineToMethodIdMap +
                ", fieldDefinitions=" + fieldDefinitions +
                ", total=" + total +
                ", covered=" + covered +
                ", rate=" + rate +
                ", methodCount=" + methodCount +
                '}';
    }

    private void extractMethodDeclarations(ClassOrInterfaceDeclaration ciAst) {
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
            CoverageDataStore.getInstance().getMethodDataMap().put(id, mData);
            methodDataIds.add(id);

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
            CoverageDataStore.getInstance().getMethodDataMap().put(id, mData);
            methodDataIds.add(id);

            for(int i = mData.getBeginLine(); i <= mData.getEndLine(); i++) {
                this.lineToMethodIdMap.put(i, id);
            }
        }

        // Add default constructor
        if (this.getMethodDataFromStore().values().stream().noneMatch(mData -> mData.getName().equals("<init>"))) {
            UUID id = UUID.randomUUID();
            MethodData mData = new MethodData(id, this.relativePath, AccessSpecifier.PUBLIC.ordinal(), "<init>", "()V;");
            CoverageDataStore.getInstance().getMethodDataMap().put(id, mData);
            methodDataIds.add(id);

            this.lineToMethodIdMap.put(Integer.MIN_VALUE, id);
        }

        JDFCUtils.logThis(JDFCUtils.prettyPrintMap(lineToMethodIdMap), "lineToMethodIdMap");
    }
}
