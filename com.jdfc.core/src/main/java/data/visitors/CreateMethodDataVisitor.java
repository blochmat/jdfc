package data.visitors;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import data.ClassData;
import data.MethodData;
import data.ProjectData;
import org.objectweb.asm.MethodVisitor;

import java.util.Optional;

import static org.objectweb.asm.Opcodes.ASM5;

public class CreateMethodDataVisitor extends MethodVisitor {

    private MethodData methodData;
    private ClassData classData;
    private ClassOrInterfaceDeclaration classDeclaration;

    public CreateMethodDataVisitor(MethodVisitor methodVisitor,
                                   MethodData methodData,
                                   ClassData classData,
                                   ClassOrInterfaceDeclaration classDeclaration) {
        super(ASM5, methodVisitor);
        this.methodData = methodData;
        this.classData = classData;
        this.classDeclaration = classDeclaration;
    }

    @Override
    public void visitEnd() {
        MethodDeclaration methodDeclaration = this.findMethodDeclaration();
        ConstructorDeclaration constructorDeclaration = this.findConstructorDeclaration();

        if (methodDeclaration != null) {
            this.methodData.setBeginLine(this.extractBegin(methodDeclaration));
            this.methodData.setEndLine(this.extractEnd(methodDeclaration));
            this.methodData.setDeclarationStr(methodDeclaration.getDeclarationAsString());
        }

        if (constructorDeclaration != null) {
            this.methodData.setBeginLine(this.extractBegin(constructorDeclaration));
            this.methodData.setEndLine(this.extractEnd(constructorDeclaration));
            this.methodData.setDeclarationStr(constructorDeclaration.getDeclarationAsString());
        }

        // Add lines of method to map
        for(int i = this.methodData.getBeginLine(); i <= this.methodData.getEndLine(); i++) {
            this.classData.getLineToMethodIdMap().put(i, this.methodData.getId());
        }

        // Special case: Default constructor
        if (methodDeclaration == null && constructorDeclaration == null && this.methodData.getName().equals("<init>")) {
            this.methodData.setBeginLine(Integer.MIN_VALUE);
            this.methodData.setEndLine(Integer.MIN_VALUE);
            this.classData.getLineToMethodIdMap().put(Integer.MIN_VALUE, methodData.getId());
        }

        if (methodDeclaration == null && constructorDeclaration == null && !this.methodData.getName().equals("<init>")) {
            throw new IllegalArgumentException(String.format("Missing method declaration in class %s: %s",
                    classData.getClassMetaData().getClassFileRel(), methodData.buildInternalMethodName()));
        }

        this.classData.getMethodDataIds().add(this.methodData.getId());
        ProjectData.getInstance().getMethodDataMap().put(this.methodData.getId(), this.methodData);
        super.visitEnd();
    }

    // --- Private Methods ---------------------------------------------------------------------------------------------
    private MethodDeclaration findMethodDeclaration() {
        for (MethodDeclaration methodDeclaration : this.classDeclaration.getMethods()) {
            int srcBeginLine = this.extractBegin(methodDeclaration);
            int srcEndLine = this.extractEnd(methodDeclaration);
            if (srcBeginLine <= this.methodData.getBeginLine()
                    && this.methodData.getEndLine() <= srcEndLine) {
                return methodDeclaration;
            }
        }
        return null;
    }

    private int extractBegin(MethodDeclaration methodDeclaration) {
        Optional<Position> posOpt = methodDeclaration.getBegin();
        if(posOpt.isPresent()) {
            return posOpt.get().line;
        } else {
            throw new IllegalArgumentException("Method begin is undefined.");
        }
    }

    private int extractEnd(MethodDeclaration methodDeclaration) {
        Optional<Position> posOpt = methodDeclaration.getEnd();
        if(posOpt.isPresent()) {
            return posOpt.get().line;
        } else {
            throw new IllegalArgumentException("Method end is undefined.");
        }
    }

    private ConstructorDeclaration findConstructorDeclaration() {
        for (ConstructorDeclaration constructorDeclaration : this.classDeclaration.getConstructors()) {
            int srcBeginLine = this.extractBegin(constructorDeclaration);
            int srcEndLine = this.extractEnd(constructorDeclaration);
            if (srcBeginLine <= this.methodData.getBeginLine()
                    && this.methodData.getEndLine() <= srcEndLine) {
                return constructorDeclaration;
            }
        }
        return null;
    }

    private int extractBegin(ConstructorDeclaration srcAst) {
        Optional<Position> posOpt = srcAst.getBegin();
        if(posOpt.isPresent()) {
            return posOpt.get().line;
        } else {
            throw new IllegalArgumentException("Method begin is undefined.");
        }
    }

    private int extractEnd(ConstructorDeclaration srcAst) {
        Optional<Position> posOpt = srcAst.getEnd();
        if(posOpt.isPresent()) {
            return posOpt.get().line;
        } else {
            throw new IllegalArgumentException("Method end is undefined.");
        }
    }
}
