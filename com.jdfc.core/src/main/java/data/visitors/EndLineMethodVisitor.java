package data.visitors;

import data.MethodData;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ASM5;

public class EndLineMethodVisitor extends MethodVisitor {

    private final MethodData methodData;
    private int endLine;

    public EndLineMethodVisitor(MethodVisitor methodVisitor, MethodData methodData) {
        super(ASM5, methodVisitor);
        this.methodData = methodData;
        this.endLine = 0;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (this.endLine == 0 | this.endLine < line) {
            this.endLine = line;
        }
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitEnd() {
        this.methodData.setEndLine(this.endLine);
        super.visitEnd();
    }
}
