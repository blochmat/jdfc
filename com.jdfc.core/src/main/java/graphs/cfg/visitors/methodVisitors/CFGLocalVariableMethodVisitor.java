package graphs.cfg.visitors.methodVisitors;

import data.singleton.CoverageDataStore;
import graphs.cfg.LocalVariable;
import graphs.cfg.visitors.classVisitors.CFGLocalVariableClassVisitor;
import instr.methodVisitors.JDFCMethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.objectweb.asm.Opcodes.ASM5;

public class CFGLocalVariableMethodVisitor extends JDFCMethodVisitor {

    private final Logger logger = LoggerFactory.getLogger(CFGLocalVariableMethodVisitor.class);

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
        logger.debug("visitLocalVariable");
        super.visitLocalVariable(pName, pDescriptor, pSignature, pStart, pEnd, pIndex);
        UUID lId = UUID.randomUUID();
        final LocalVariable lVar = new LocalVariable(pName, pDescriptor, pSignature, pIndex);
        localVariableTable.put(pIndex, lVar);
        CoverageDataStore.getInstance().getUuidLocalVariableMap().put(lId, lVar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        logger.debug("visitEnd");
        classVisitor.classExecutionData.getMethodByInternalName(internalMethodName)
                .setLocalVariableTable(localVariableTable);
    }
}
