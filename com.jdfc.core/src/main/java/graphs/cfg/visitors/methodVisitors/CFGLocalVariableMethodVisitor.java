package graphs.cfg.visitors.methodVisitors;

import graphs.cfg.LocalVariable;
import graphs.cfg.visitors.classVisitors.CFGLocalVariableClassVisitor;
import instr.methodVisitors.JDFCMethodVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

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
        classVisitor.classExecutionData.getMethodByInternalName(internalMethodName)
                .setLocalVariableTable(localVariableTable);
    }
}
