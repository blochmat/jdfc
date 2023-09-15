package graphs.cfg.visitors.methodVisitors;

import com.google.common.base.Preconditions;
import data.MethodData;
import graphs.cfg.LocalVariable;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM5;

@Slf4j
public class CFGLocalVariableMethodVisitor extends MethodVisitor {

    private final Map<Integer, LocalVariable> localVariableTable = new HashMap<>();
    private final MethodData methodData;

    public CFGLocalVariableMethodVisitor(MethodData methodData) {
        super(ASM5);
        Preconditions.checkNotNull(methodData);
        this.methodData = methodData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(
            final String name,
            final String descriptor,
            final String signature,
            final Label start,
            final Label end,
            final int index) {
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
        final LocalVariable variable = new LocalVariable(name, descriptor, signature, index);
        this.localVariableTable.put(index, variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        this.methodData.setLocalVariableTable(localVariableTable);
    }
}
