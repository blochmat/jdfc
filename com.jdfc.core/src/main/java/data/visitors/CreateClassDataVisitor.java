package data.visitors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import data.ClassData;
import data.MethodData;
import data.singleton.CoverageDataStore;
import instr.ClassMetaData;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import utils.ASMHelper;
import utils.JDFCUtils;
import utils.JavaParserHelper;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.objectweb.asm.Opcodes.ASM5;

public class CreateClassDataVisitor extends ClassVisitor {

    private final ClassData classData;
    private final ClassMetaData classMetaData;
    private final ClassNode classNode;
    private final ClassOrInterfaceDeclaration classDeclaration;
    private final ASMHelper asmHelper;

    public CreateClassDataVisitor(ClassNode classNode, ClassMetaData classMetaData) {
        super(ASM5);
        this.classMetaData = classMetaData;
        this.classNode = classNode;
        this.asmHelper = new ASMHelper();

        Map<String, String> nestedTypeMap;
        try {
            JavaParserHelper javaParserHelper = new JavaParserHelper();
            CompilationUnit compilationUnit = javaParserHelper.parse(this.classMetaData.getSourceFile());
            this.classDeclaration = this.extractClassDeclaration(compilationUnit);
            nestedTypeMap = this.extractNestedTypes(compilationUnit);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("ERROR: Missing source file for " + this.classMetaData.getClassFileAbs());
        }

        UUID id = UUID.randomUUID();
        classMetaData.setClassDataId(id);
        this.classData = new ClassData(id, classMetaData, nestedTypeMap);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        MethodNode methodNode = this.asmHelper.getMethodNode(this.classNode, name, descriptor, signature);
        String internalMethodName = this.asmHelper.computeInternalMethodName(name, descriptor, signature, exceptions);
        if (methodNode != null && this.asmHelper.isInstrumentationRequired(methodNode, internalMethodName)) {
            // Create MethodData
            UUID id = UUID.randomUUID();
            MethodData methodData = new MethodData(id, this.classMetaData.getFqn(), access, internalMethodName);

            // Gather meta data for method and save
            mv = new CreateMethodDataVisitor(mv, methodData, this.classData, this.classDeclaration);
            mv = new EndLineMethodVisitor(mv, methodData);
            mv = new BeginLineMethodVisitor(mv, methodData);
        }

        return mv;
    }

    @Override
    public void visitEnd() {
        CoverageDataStore.getInstance().getClassDataMap().put(this.classData.getId(), this.classData);
        CoverageDataStore.getInstance().getPackageDataMap().get(this.classMetaData.getClassFilePackageRel()).getClassDataIds().add(this.classData.getId());
        super.visitEnd();
    }

    // --- Private Methods ---------------------------------------------------------------------------------------------
    private ClassOrInterfaceDeclaration extractClassDeclaration(CompilationUnit compilationUnit) {
        Optional<ClassOrInterfaceDeclaration> ciOptional;
        if(this.classMetaData.isInnerClass()) {
            ciOptional = compilationUnit.getClassByName(this.classMetaData.getOuterName());
            if (ciOptional.isPresent()) {
                ClassOrInterfaceDeclaration ci = ciOptional.get();
                for (BodyDeclaration<?> body : ci.getMembers()) {
                    if (body.isClassOrInterfaceDeclaration()) {
                        ClassOrInterfaceDeclaration ciInner = (ClassOrInterfaceDeclaration) body;
                        if(ciInner.getName().asString().equals(this.classMetaData.getName())) {
                            return ciInner;
                        }
                    }
                }
            }
            throw new IllegalArgumentException(String.format("Class \"%s\" is not present in file \"%s\".",
                    classMetaData, classMetaData.getClassFileRel()));
        } else {
            ciOptional = compilationUnit.getClassByName(this.classMetaData.getName());
            if (ciOptional.isPresent()) {
                return ciOptional.get();
            } else {
                throw new IllegalArgumentException(String.format("Class \"%s\" is not present in file \"%s\".",
                        classMetaData, classMetaData.getClassFileRel()));
            }
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

}
