package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
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
public class ClassExecutionData extends ExecutionData {

    @JsonIgnore
    private CompilationUnit srcFileAst;

    @JsonIgnore
    private PackageDeclaration pkgDecl;

    @JsonIgnore
    private List<ImportDeclaration> impDeclList;

    @JsonIgnore
    private ClassOrInterfaceDeclaration ciDecl;

    private UUID id;

    private String relativePath;

    private Map<String, String> nestedTypeMap;

    private Set<ProgramVariable> fields;

    private Map<UUID, MethodData> methods;

    private Map<Integer, UUID> lineToMethodIdMap;

    public ClassExecutionData(String fqn, String name, UUID id, String pRelativePath, CompilationUnit srcFileAst) {
        super(fqn, name);
        this.fields = new HashSet<>();
        this.lineToMethodIdMap = new HashMap<>();
        this.id = id;
        this.relativePath = pRelativePath;
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
            String jvmDesc = javaParserHelper.toJvmDescriptor(mDecl);

            Set<Type> types = new HashSet<>();
            // Add return, param and exception types
            types.add(mDecl.getType());
            types.addAll(mDecl.getParameters().stream().map(Parameter::getType).collect(Collectors.toSet()));
            types.addAll(mDecl.getThrownExceptions().stream().map(ReferenceType::asReferenceType).collect(Collectors.toSet()));
            // add full relative paths: (I)Lcom/jdfc/Option$Builder; [java/lang/IndexOutOfBoundsException]
            Set<ResolvedType> resolvedTypes = types.stream().map(Type::resolve).collect(Collectors.toSet());
            String jvmAsmDesc = javaParserHelper.buildJvmAsmDesc(resolvedTypes, nestedTypeMap, jvmDesc, new HashSet<>(),
                    new HashSet<>());

            int mAccess = mDecl.getAccessSpecifier().ordinal();
            String mName = mDecl.getName().getIdentifier();

            UUID id = UUID.randomUUID();
            MethodData mData = new MethodData(id, mAccess, mName, jvmAsmDesc, mDecl);

            methods.put(id, mData);
            for(int i = mData.getBeginLine(); i <= mData.getEndLine(); i++) {
                this.lineToMethodIdMap.put(i, id);
            }
            JDFCUtils.logThis(JDFCUtils.prettyPrintMap(lineToMethodIdMap), "lineToMethodIdMap");
        }

        for(ConstructorDeclaration cDecl : ciAst.getConstructors()) {
            JavaParserHelper javaParserHelper = new JavaParserHelper();
            // jvm patter built from JavaParser: (I)LBuilder; [IndexOutOfBoundsException]
            String jvmDesc = javaParserHelper.toJvmDescriptor(cDecl);

            Set<Type> types = new HashSet<>();
            types.addAll(cDecl.getParameters().stream().map(Parameter::getType).collect(Collectors.toSet()));
            types.addAll(cDecl.getThrownExceptions().stream().map(ReferenceType::asReferenceType).collect(Collectors.toSet()));

            // add full relative paths: (I)Lcom/jdfc/Option$Builder; [java/lang/IndexOutOfBoundsException]
            Set<ResolvedType> resolvedTypes = types.stream().map(Type::resolve).collect(Collectors.toSet());
            String jvmAsmDesc = javaParserHelper.buildJvmAsmDesc(resolvedTypes, nestedTypeMap, jvmDesc, new HashSet<>(),
                    new HashSet<>());
            int mAccess = cDecl.getAccessSpecifier().ordinal();
            String mName = "<init>";
            UUID id = UUID.randomUUID();
            MethodData mData = new MethodData(id, mAccess, mName, jvmAsmDesc, cDecl);
            methods.put(id, mData);

            for(int i = mData.getBeginLine(); i <= mData.getEndLine(); i++) {
                this.lineToMethodIdMap.put(i, id);
            }
            JDFCUtils.logThis(JDFCUtils.prettyPrintMap(lineToMethodIdMap), "lineToMethodIdMap");
        }

        // Add default constructor
        if (methods.values().stream().noneMatch(x -> x.getName().equals("<init>") && x.getDesc().equals("()V;"))) {
            UUID id = UUID.randomUUID();
            MethodData mData = new MethodData(id, AccessSpecifier.PUBLIC.ordinal(), "<init>", "()V;");

            methods.put(id, mData);
        }

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

    public MethodData getMethodByShortInternalName(String shortInternalName) {
        for(MethodData mData : methods.values()) {
            if (mData.buildInternalMethodName().contains(shortInternalName)) {
                return mData;
            }
        }

        if(log.isDebugEnabled()) {
            File transformFile = JDFCUtils.createFileInDebugDir("getMethodByShortInternalName.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                writer.write(String.format("Search param: %s", shortInternalName));
                writer.write("\n");
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
        for (MethodData mData : this.getMethods().values()) {
            JDFCUtils.logThis(mData.getName() + "\n" + JDFCUtils.prettyPrintMap(mData.getProgramVariables()), "programVariables");
            String internalMethodName = mData.buildInternalMethodName();
            if (mData.getPairs().size() == 0) {
                continue;
            }

            for (DefUsePair pair : mData.getPairs()) {
                ProgramVariable def = pair.getDefinition();
                ProgramVariable use = pair.getUsage();

                if (def.isCovered() && use.isCovered()) {
                    if (!internalMethodName.contains("<clinit>")) {
                        this.getMethodByInternalName(internalMethodName).findDefUsePair(pair).setCovered(true);
                    }
                } else {
                    if (!internalMethodName.contains("<clinit>")) {
                        this.getMethodByInternalName(internalMethodName).findDefUsePair(pair).setCovered(false);
                    }
                }
                if (!internalMethodName.contains("<clinit>")) {
                    mData.computeCoverage();
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
