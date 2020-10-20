package com.jdfc.core.analysis;

import com.jdfc.core.analysis.ifg.data.LocalVariableTable;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public abstract class JDFCMethodVisitor extends MethodVisitor {

    public final JDFCClassVisitor classVisitor;
    public final String methodName;
    public final LocalVariableTable localVariableTable;
    public int currentLineNumber = -1;
    public int firstLine = -1;
    public String classDescriptor;

    public JDFCMethodVisitor(final int pApi,
                             final JDFCClassVisitor pClassVisitor,
                             final MethodVisitor pMethodVisitor,
                             final String pMethodName) {
        super(pApi, pMethodVisitor);
        classVisitor = pClassVisitor;
        methodName = pMethodName;
        classDescriptor = String.format("L%s;", classVisitor.classExecutionData.getRelativePath());
        localVariableTable = new LocalVariableTable();
    }

    public JDFCMethodVisitor(final int pApi,
                             final JDFCClassVisitor pClassVisitor,
                             final MethodVisitor pMethodVisitor,
                             final String pMethodName,
                             final LocalVariableTable pLocalVariableTable) {
        super(pApi, pMethodVisitor);
        classVisitor = pClassVisitor;
        methodName = pMethodName;
        classDescriptor = String.format("L%s;", classVisitor.classExecutionData.getRelativePath());
        localVariableTable = pLocalVariableTable;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (firstLine == -1) {
            if (methodName.equals("<init>")) {
                firstLine = line;
            } else {
                firstLine += line;
            }
        }
        currentLineNumber = line;
        super.visitLineNumber(line, start);
    }


    protected boolean isStandardConstructor(String pDescriptor) {
        return methodName.equals("<init>")
                && localVariableTable.size() == 1
                && localVariableTable.containsEntry("this", pDescriptor);
    }
}
