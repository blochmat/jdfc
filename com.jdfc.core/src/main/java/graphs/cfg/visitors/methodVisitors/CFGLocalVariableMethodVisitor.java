package graphs.cfg.visitors.methodVisitors;

import com.google.common.collect.Maps;
import data.singleton.CoverageDataStore;
import graphs.cfg.LocalVariable;
import graphs.cfg.visitors.classVisitors.CFGLocalVariableClassVisitor;
import instr.methodVisitors.JDFCMethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

import static org.objectweb.asm.Opcodes.ASM5;

public class CFGLocalVariableMethodVisitor extends JDFCMethodVisitor {

    private final Logger logger = LoggerFactory.getLogger(CFGLocalVariableMethodVisitor.class);

    private final Map<Integer, UUID> localVariableTable;

    public CFGLocalVariableMethodVisitor(final CFGLocalVariableClassVisitor classVisitor,
                                         final MethodVisitor methodVisitor,
                                         final MethodNode methodNode,
                                         final String internalMethodName) {
        super(ASM5, classVisitor, methodVisitor, methodNode, internalMethodName);
        localVariableTable = Maps.newHashMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(final String name, final String desc, final String sig, final Label start,
                                   final Label end, final int index) {
        logger.debug("visitLocalVariable");
        super.visitLocalVariable(name, desc, sig, start, end, index);
        UUID uuid = UUID.randomUUID();
        final LocalVariable lVar = new LocalVariable(index, name, desc, sig);
        localVariableTable.put(index, uuid);
        CoverageDataStore.getInstance().getUuidLocalVariableMap().put(uuid, lVar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        logger.debug("visitEnd");
        classVisitor.classExecutionData.getMethodByInternalName(internalMethodName)
                .setLocalVarIdxToUUID(localVariableTable);
    }
}
