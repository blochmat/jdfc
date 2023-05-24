package icfg.visitors.methodVisitors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import icfg.ICFG;
import icfg.ICFGCreator;
import icfg.ICFGImpl;
import icfg.data.LocalVariable;
import icfg.data.ProgramVariable;
import icfg.nodes.*;
import icfg.visitors.classVisitors.ICFGNodeClassVisitor;
import instr.methodVisitors.JDFCMethodVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class ICFGNodeMethodVisitor extends JDFCMethodVisitor {

    private final Map<String, ICFG> methodCFGs;
    private final Multimap<Double, Double> edges;
    private final NavigableMap<Double, ICFGNode> nodes;

    public ICFGNodeMethodVisitor(final ICFGNodeClassVisitor pClassVisitor,
                                 final MethodVisitor pMethodVisitor,
                                 final MethodNode pMethodNode,
                                 final String pInternalMethodName,
                                 final Map<String, ICFG> pMethodCFGs,
                                 final Map<Integer, LocalVariable> pLocalVariableTable) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName, pLocalVariableTable);
        methodCFGs = pMethodCFGs;
        edges = ArrayListMultimap.create();
        nodes = Maps.newTreeMap();
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        super.visitFrame(type, numLocal, local, numStack, stack);
        final ICFGNode node = new ICFGNode(currentInstructionIndex, getFrameOpcode(type));
        nodes.put((double) currentInstructionIndex, node);
    }

    private int getFrameOpcode(int type) {
        switch (type) {
            case -1:
                return F_NEW;
            case 0:
                return F_FULL;
            case 1:
                return F_APPEND;
            case 2:
                return F_CHOP;
            case 3:
                return F_SAME;
            case 4:
                return F_SAME1;
            default:
                return Integer.MIN_VALUE;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitFrameNew() {
        if (currentNode.getOpcode() == F_NEW) {
            final ICFGNode node = new ICFGNode(currentInstructionIndex, F_NEW);
            nodes.put((double) currentInstructionIndex, node);
            updateCurrentNode();
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        visitFrameNew();
        createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
//        createCFGNodeForFieldInsnNode(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        visitFrameNew();
        if (owner.equals(classVisitor.classNode.name) && isInstrumentationRequired(internalMethodName)) {
            final ICFGNode callNode = new ICFGCallNode(currentInstructionIndex, opcode);
            nodes.put((double) currentInstructionIndex + 0.25, callNode);
            String callSiteMethodName = computeInternalMethodName(name, descriptor);
            int paramsCount = (int) Arrays.stream(Type.getArgumentTypes(descriptor)).filter(x -> !x.toString().equals("[")).count();
//            final ToBeDeleted node = new ToBeDeleted(currentInstructionIndex, currentLineNumber, opcode, owner, null, callSiteMethodName, paramsCount);
            final ICFGNode returnNode = new ICFGReturnNode(currentInstructionIndex, opcode);
            nodes.put((double) currentInstructionIndex + 0.75, returnNode);

        } else {
            final ICFGNode node = new ICFGNode(currentInstructionIndex, opcode);
            nodes.put((double) currentInstructionIndex, node);
        }
    }

    private String computeInternalMethodName(String name, String descriptor) {
        for (MethodNode node : classVisitor.classNode.methods) {
            if (node.name.equals(name) && node.desc.equals(descriptor)) {
                return ICFGCreator.computeInternalMethodName(node.name, node.desc, node.signature, node.exceptions.toArray(new String[0]));
            }
        }
        return null;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, INVOKEDYNAMIC);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, LDC);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        visitFrameNew();
        createCFGNodeForIincInsnNode(var, currentInstructionIndex, currentLineNumber);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, TABLESWITCH);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, LOOKUPSWITCH);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        visitFrameNew();
        final ICFGNode node = new ICFGNode(currentInstructionIndex, MULTIANEWARRAY);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        edges.putAll(createEdges());
        setPredecessorSuccessorRelation();

        addEntryNode();
        ICFGNode exitNode = new ICFGExitNode(Integer.MAX_VALUE, Integer.MIN_VALUE );
        nodes.put((double) Integer.MAX_VALUE, exitNode);

        boolean isImpure = false;
        for (Map.Entry<Double, ICFGNode> nodeEntry : nodes.entrySet()) {
            double instrIdx = nodeEntry.getKey();
            ICFGNode node = nodeEntry.getValue();
            System.out.printf("%f: %s%n", instrIdx, node.toString());
        }
        ICFG ICFG = new ICFGImpl(internalMethodName, nodes, localVariableTable, isImpure);
        methodCFGs.put(internalMethodName, ICFG);
        classVisitor.classExecutionData.getMethodFirstLine().put(internalMethodName, firstLine);
        classVisitor.classExecutionData.getMethodLastLine().put(internalMethodName, currentLineNumber);
    }

    private void createCFGNodeForVarInsnNode(final int opcode, final int varNumber, final int pIndex, final int lineNumber) {
        final ICFGNode node;
        final ProgramVariable programVariable;
        switch (opcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                programVariable = getProgramVariableFromLocalVar(varNumber, opcode, pIndex, lineNumber);
                node = new ICFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), pIndex, opcode);
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                programVariable = getProgramVariableFromLocalVar(varNumber, opcode, pIndex, lineNumber);
                node = new ICFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), pIndex, opcode);
                break;
            default:
                node = new ICFGNode(pIndex, opcode);
                break;
        }
        nodes.put((double) pIndex, node);
    }

    private void createCFGNodeForIincInsnNode(final int varNumber, final int pIndex, final int pLineNumber) {
        final ProgramVariable programVariable = getProgramVariableFromLocalVar(varNumber, ISTORE, pIndex, pLineNumber);
        final ICFGNode node =
                new ICFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex, IINC);
        nodes.put((double) pIndex, node);
    }

    private Multimap<Double, Double> createEdges() {
        ICFGEdgeAnalysisVisitor cfgEdgeAnalysationVisitor =
                new ICFGEdgeAnalysisVisitor(methodNode);
        methodNode.accept(cfgEdgeAnalysationVisitor);
        return cfgEdgeAnalysationVisitor.getEdges();
    }

    private void setPredecessorSuccessorRelation() {
        for (Map.Entry<Double, Double> edge : edges.entries()) {
            final ICFGNode first = nodes.get(edge.getKey());
            final ICFGNode second = nodes.get(edge.getValue());
            first.addSuccessor(second);
            second.addPredecessor(first);
        }
    }

    private void addEntryNode() {
        final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();

        for (LocalVariable localVariable : localVariableTable.values()) {
            final ProgramVariable variable =
                    ProgramVariable.create(null,
                            localVariable.getName(),
                            localVariable.getDescriptor(),
                            Integer.MIN_VALUE,
                            firstLine,
                            true);
            parameters.add(variable);
        }

        final ICFGNode firstNode = nodes.get(0);
        if (firstNode != null) {
            final ICFGNode entryNode =
                    new ICFGEntryNode(
                            parameters,
                            Sets.newLinkedHashSet(),
                            Integer.MIN_VALUE,
                            Integer.MIN_VALUE,
                            Sets.newLinkedHashSet(),
                            Sets.newHashSet(firstNode));
            firstNode.addPredecessor(entryNode);
            nodes.put((double) Integer.MIN_VALUE, entryNode);
        }

    }
}
