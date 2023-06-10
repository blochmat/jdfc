package graphs.cfg.visitors.methodVisitors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.MethodData;
import data.ProgramVariable;
import graphs.cfg.CFG;
import graphs.cfg.CFGImpl;
import graphs.cfg.LocalVariable;
import graphs.cfg.nodes.CFGEntryNode;
import graphs.cfg.nodes.CFGExitNode;
import graphs.cfg.nodes.CFGNode;
import graphs.cfg.visitors.classVisitors.CFGNodeClassVisitor;
import instr.methodVisitors.JDFCMethodVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.UUID;

import static org.objectweb.asm.Opcodes.*;

public class CFGNodeMethodVisitor extends JDFCMethodVisitor {
    private final Logger logger = LoggerFactory.getLogger(CFGNodeMethodVisitor.class);
    private final Multimap<Double, Double> edges;
    private final NavigableMap<Double, CFGNode> nodes;
    private final MethodData mData;

    public CFGNodeMethodVisitor(final CFGNodeClassVisitor pClassVisitor,
                                final MethodVisitor pMethodVisitor,
                                final MethodNode pMethodNode,
                                final String pInternalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
        logger.debug(String.format("Visiting %s", pInternalMethodName));
        edges = ArrayListMultimap.create();
        nodes = Maps.newTreeMap();
        mData = pClassVisitor.classExecutionData.getMethodByInternalName(internalMethodName);
        logger.debug(JDFCUtils.prettyPrintMap(mData.getLocalVariableTable()));
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        logger.debug("visitFrame");
        super.visitFrame(type, numLocal, local, numStack, stack);
        final CFGNode node = new CFGNode(currentInstructionIndex, getFrameOpcode(type));
        nodes.put((double) currentInstructionIndex, node);
    }

    private int getFrameOpcode(int type) {
        logger.debug("getFrameOpcode");
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
        String debug = String.format("visitInsn %s", JDFCUtils.getOpcode(opcode));
        logger.debug(debug);
        super.visitInsn(opcode);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitFrameNew() {
        logger.debug("visitFrameNew");
        if (currentNode.getOpcode() == F_NEW) {
            final CFGNode node = new CFGNode(currentInstructionIndex, F_NEW);
            nodes.put((double) currentInstructionIndex, node);
            updateCurrentNode();
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        String debug = String.format("visitIntInsn %s", JDFCUtils.getOpcode(opcode));
        logger.debug(debug);
        super.visitIntInsn(opcode, operand);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        String debug = String.format("visitVarInsn %s", JDFCUtils.getOpcode(opcode));
        logger.debug(debug);
        super.visitVarInsn(opcode, var);
        visitFrameNew();
        createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        String debug = String.format("visitTypeInsn %s", JDFCUtils.getOpcode(opcode));
        logger.debug(debug);
        super.visitTypeInsn(opcode, type);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        String debug = String.format("visitFieldInsn %s", JDFCUtils.getOpcode(opcode));
        logger.debug(debug);
        super.visitFieldInsn(opcode, owner, name, descriptor);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        String debug = String.format("visitMethodInsn %s", JDFCUtils.getOpcode(opcode));
        logger.debug(debug);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        logger.debug("visitInvokeDynamicInsn");
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, INVOKEDYNAMIC);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        String debug = String.format("visitJumpInsn %s %s", JDFCUtils.getOpcode(opcode), label);
        logger.debug(debug);
        super.visitJumpInsn(opcode, label);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitLdcInsn(Object value) {
        logger.debug("visitLdcInsn");
        super.visitLdcInsn(value);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, LDC);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        logger.debug("visitIincInsn");
        super.visitIincInsn(var, increment);
        visitFrameNew();
        createCFGNodeForIincInsnNode(var, currentInstructionIndex, currentLineNumber);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        logger.debug("visitTableSwitchInsn");
        super.visitTableSwitchInsn(min, max, dflt, labels);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, TABLESWITCH);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        logger.debug("visitLookupSwitchInsn");
        super.visitLookupSwitchInsn(dflt, keys, labels);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, LOOKUPSWITCH);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        logger.debug("visitMultiANewArrayInsn");
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, MULTIANEWARRAY);
        nodes.put((double) currentInstructionIndex, node);
    }

    @Override
    public void visitEnd() {
        logger.debug("visitEnd");
        super.visitEnd();

        edges.putAll(createEdges());
        this.addEntryAndExitNode();
        this.setPredecessorSuccessorRelation();
        CFG cfg = new CFGImpl(internalMethodName, nodes, edges);

        logger.debug(internalMethodName);
        logger.debug(JDFCUtils.prettyPrintMultimap(edges));

        if (!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
            MethodData mData = classVisitor.classExecutionData.getMethodByInternalName(internalMethodName);
            mData.setParams(cfg.getNodes().get((double) Integer.MIN_VALUE).getDefinitions());
            mData.setCfg(cfg);
            mData.getCfg().calculateReachingDefinitions();
            mData.calculateDefUsePairs();
        } else {
            // TODO: <init>: ()V is not in methods
        }
    }

    private ProgramVariable getProgramVariableFromLocalVar(final int varNumber,
                                                           final int pOpcode,
                                                           final int pIndex,
                                                           final int pLineNumber) {
        logger.debug(String.format("getProgramVariableFromLocalVar(%d, %d, %d, %d)", varNumber, pOpcode, pIndex, pLineNumber));
        final String varName = getLocalVarName(varNumber);
        final String varType = getLocalVarType(varNumber);
        final boolean isDefinition = isDefinition(pOpcode);
        ProgramVariable var = new ProgramVariable(null, varName, varType, pIndex, pLineNumber, isDefinition, false);
        UUID id = UUID.randomUUID();
        mData.getPVarToUUIDMap().put(id, var);
        return var;
    }

    private String getLocalVarName(final int pVarNumber) {
        logger.debug(String.format("getLocalVarName(%d)", pVarNumber));
        final LocalVariable localVariable = mData.getLocalVariableTable().get(pVarNumber);
        if (localVariable != null) {
            return localVariable.getName();
        } else {
            return String.valueOf(pVarNumber);
        }
    }

    private String getLocalVarType(final int pVarNumber) {
        logger.debug(String.format("getLocalVarType(%d)", pVarNumber));
        final LocalVariable localVariable = mData.getLocalVariableTable().get(pVarNumber);
        if (localVariable != null) {
            return localVariable.getDescriptor();
        } else {
            return "UNKNOWN";
        }
    }

    private void createCFGNodeForVarInsnNode(final int opcode, final int varNumber, final int pIndex, final int lineNumber) {
        logger.debug("createCFGNodeForVarInsnNode");
        final CFGNode node;
        final ProgramVariable programVariable;
        switch (opcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                programVariable = getProgramVariableFromLocalVar(varNumber, opcode, pIndex, lineNumber);
                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), pIndex, opcode);
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                programVariable = getProgramVariableFromLocalVar(varNumber, opcode, pIndex, lineNumber);
                node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), pIndex, opcode);
                break;
            default:
                node = new CFGNode(pIndex, opcode);
                break;
        }
        nodes.put((double) pIndex, node);
    }

    private void createCFGNodeForIincInsnNode(final int varNumber, final int pIndex, final int pLineNumber) {
        logger.debug("createCFGNodeForIincInsnNode");
        final ProgramVariable programVariable = getProgramVariableFromLocalVar(varNumber, ISTORE, pIndex, pLineNumber);
        final CFGNode node =
                new CFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex, IINC);
        nodes.put((double) pIndex, node);
    }

    private Multimap<Double, Double> createEdges() {
        logger.debug("createEdges");
        CFGEdgeAnalysisVisitor cfgEdgeAnalysisVisitor =
                new CFGEdgeAnalysisVisitor(methodNode);
        methodNode.accept(cfgEdgeAnalysisVisitor);
        return cfgEdgeAnalysisVisitor.getEdges();
    }

    private void setPredecessorSuccessorRelation() {
        logger.debug("setPredecessorSuccessorRelation");
        for (Map.Entry<Double, Double> edge : edges.entries()) {
            final CFGNode first = nodes.get(edge.getKey());
            final CFGNode second = nodes.get(edge.getValue());
            first.addSuccessor(second);
            second.addPredecessor(first);
        }
    }

    private void addEntryAndExitNode() {
        logger.debug("addEntryAndExitNode");
        Set<ProgramVariable> parameters = createParamVars();

        final CFGNode entryNode =
                new CFGEntryNode(parameters, Sets.newLinkedHashSet(), Sets.newLinkedHashSet(), Sets.newLinkedHashSet());
        nodes.put((double) Integer.MIN_VALUE, entryNode);
        final CFGNode exitNode =
                new CFGExitNode(Sets.newLinkedHashSet(), Sets.newLinkedHashSet(), Sets.newHashSet(), Sets.newLinkedHashSet());
        nodes.put((double) Integer.MAX_VALUE, exitNode);

        if(nodes.size() == 2) {
            // only contains entry and exit nodes
            edges.put((double) Integer.MIN_VALUE, (double) Integer.MAX_VALUE);
        } else {
            // Add edge from entry node
            final CFGNode firstNode = nodes.get(0.0);
            if(firstNode == null) {
                throw new RuntimeException("Add entry node failed, because first node was null.");
            }
            edges.put((double) Integer.MIN_VALUE, 0.0);

            // Add edges to exit node
            for (Map.Entry<Double, CFGNode> nodeEntry : nodes.entrySet()) {
                if (172 <= nodeEntry.getValue().getOpcode() && nodeEntry.getValue().getOpcode() <= 177) {
                    edges.put(nodeEntry.getKey(), (double) Integer.MAX_VALUE);
                }
            }
        }
    }

    private Set<ProgramVariable> createParamVars() {
        final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();
        for (LocalVariable localVariable : mData.getLocalVariableTable().values()) {
            final ProgramVariable variable =
                    new ProgramVariable(null,
                            localVariable.getName(),
                            localVariable.getDescriptor(),
                            Integer.MIN_VALUE,
                            Integer.MIN_VALUE,
                            true,
                            false);
            parameters.add(variable);
        }
       return parameters;
    }
}
