package graphs.cfg.visitors.methodVisitors;

import graphs.cfg.LocalVariable;
import graphs.cfg.visitors.classVisitors.CFGLocalVariableClassVisitor;
import instr.methodVisitors.JDFCMethodVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM5;

@Slf4j
public class CFGLocalVariableMethodVisitor extends JDFCMethodVisitor {

    private final Map<Integer, LocalVariable> localVariableTable = new HashMap<>();

    public CFGLocalVariableMethodVisitor(
            final CFGLocalVariableClassVisitor pClassVisitor,
            final MethodVisitor pMethodVisitor,
            final MethodNode pMethodNode,
            final String pInternalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(
            final String pName,
            final String pDescriptor,
            final String pSignature,
            final Label pStart,
            final Label pEnd,
            final int pIndex) {
        super.visitLocalVariable(pName, pDescriptor, pSignature, pStart, pEnd, pIndex);
        final LocalVariable variable = new LocalVariable(pName, pDescriptor, pSignature, pIndex);
        localVariableTable.put(pIndex, variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        if(classVisitor.classExecutionData.getMethodByInternalName(internalMethodName) == null) {
            System.err.println("ERROR: getMethodByInternalName returned null! See /target/jdfc/debug/ERROR_CFGLocalVariableMethodVisitor.log for more info.");
            if(log.isDebugEnabled()) {
                File file = JDFCUtils.createFileInDebugDir("ERROR_CFGLocalVariableMethodVisitor.log", false);
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write(String.format("Class: %s\n", classVisitor.classExecutionData.getName()));
                    writer.write(String.format("Method: %s\n", internalMethodName));
                    writer.write("==============================\n");
                    writer.write("Local Variables:\n");
                    writer.write(JDFCUtils.prettyPrintMap(classVisitor.classExecutionData.getMethodByInternalName(internalMethodName)
                            .getLocalVariableTable()));
                    writer.write("==============================\n");
                    writer.write("\n");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        } else {
            classVisitor.classExecutionData.getMethodByInternalName(internalMethodName)
                    .setLocalVariableTable(localVariableTable);
        }
    }
}
