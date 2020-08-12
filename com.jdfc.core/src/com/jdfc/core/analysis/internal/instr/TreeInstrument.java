package com.jdfc.core.analysis.internal.instr;

import com.jdfc.core.analysis.cfg.CFGImpl;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class TreeInstrument {

    private final ClassNode classNode;

    public TreeInstrument(final ClassNode pClassNode) {
        classNode = pClassNode;
    }

    /**
     * Instruments each method with calls to our execution tracer.
     */
    public void instrument() {
        for (final MethodNode methodNode : classNode.methods) {
            instrumentMethod(methodNode);
        }
    }

    private void instrumentMethod(final MethodNode pMethodNode) {
        InsnList buffer = cloneInsnList(pMethodNode.instructions);
        for (AbstractInsnNode instruction : buffer.toArray()) {
            switch (instruction.getOpcode()) {
                case Opcodes.ISTORE:
                case Opcodes.DSTORE:
                case Opcodes.FSTORE:
                case Opcodes.LSTORE:
                case Opcodes.ILOAD:
                case Opcodes.DLOAD:
                case Opcodes.FLOAD:
                case Opcodes.LLOAD:
                    VarInsnNode node = (VarInsnNode) instruction;
                    LdcInsnNode loadName = new LdcInsnNode(pMethodNode.name);
                    LdcInsnNode loadDesc = new LdcInsnNode(pMethodNode.desc);
                    LdcInsnNode loadVarIndex = new LdcInsnNode(node.var);
                    LdcInsnNode loadInstructionIndex = new LdcInsnNode(buffer.indexOf(instruction));
                    pMethodNode.instructions.insert(instruction, loadName);
                    pMethodNode.instructions.insert(loadName, loadDesc);
                    pMethodNode.instructions.insert(loadDesc, loadVarIndex);
                    pMethodNode.instructions.insert(loadVarIndex, loadInstructionIndex);
                    pMethodNode.instructions.insert(
                            loadInstructionIndex,
                            new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    Type.getInternalName(CFGImpl.class),
                                    "addCoveredEntry",
                                    "(Ljava/lang/String;Ljava/lang/String;II)V",
                                    false));
            }
        }
    }

    /**
     * Helper method to be clone InsnList
     *
     * @param toClone Instruction List that should be cloned
     * @return Instruction list clone
     */
    private InsnList cloneInsnList(InsnList toClone) {
        InsnList temp = new InsnList();
        for (AbstractInsnNode node : toClone.toArray()) {
            temp.add(node);
        }
        return temp;
    }
}
