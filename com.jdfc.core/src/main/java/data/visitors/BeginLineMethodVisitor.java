package data.visitors;

import data.MethodData;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ASM5;

public class BeginLineMethodVisitor extends MethodVisitor {

    private final MethodData methodData;
    private int beginLine;

    public BeginLineMethodVisitor(MethodVisitor methodVisitor, MethodData methodData) {
        super(ASM5, methodVisitor);
        this.methodData = methodData;
        this.beginLine = 0;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (this.beginLine == 0) {
            this.beginLine = line;
        }
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitEnd() {
        this.methodData.setBeginLine(this.beginLine);
        super.visitEnd();
    }
}
