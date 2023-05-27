package cfg.visitors.methodVisitors;

import cfg.CFG;
import cfg.CFGCreator;
import cfg.CFGImpl;
import cfg.data.LocalVariable;
import cfg.nodes.CFGNode;
import cfg.visitors.classVisitors.CFGNodeClassVisitor;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.ProgramVariable;
import icfg.nodes.ICFGEntryNode;
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

import static org.objectweb.asm.Opcodes.*;

public class CFGNodeMethodVisitor extends JDFCMethodVisitor {
    private final Logger logger = LoggerFactory.getLogger(CFGNodeMethodVisitor.class);
    private final Map<String, CFG> methodCFGs;
    private final Multimap<Double, Double> edges;
    private final NavigableMap<Double, CFGNode> nodes;
    private final Set<Double> crNodes;

    public CFGNodeMethodVisitor(final CFGNodeClassVisitor pClassVisitor,
                                final MethodVisitor pMethodVisitor,
                                final MethodNode pMethodNode,
                                final String pInternalMethodName,
                                final Map<String, CFG> pMethodCFGs,
                                final Map<Integer, LocalVariable> pLocalVariableTable) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName, pLocalVariableTable);
        logger.debug(String.format("Visiting %s", pInternalMethodName));
        methodCFGs = pMethodCFGs;
        edges = ArrayListMultimap.create();
        nodes = Maps.newTreeMap();
        crNodes = Sets.newHashSet();
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
//        createCFGNodeForFieldInsnNode(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        String debug = String.format("visitMethodInsn %s", JDFCUtils.getOpcode(opcode));
        logger.debug(debug);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        visitFrameNew();
//        if (owner.equals(classVisitor.classNode.name) && isInstrumentationRequired(internalMethodName)) {
//            String calledMethodName = computeInternalMethodName(name, descriptor);
//            int paramsCount = (int) Arrays.stream(Type.getArgumentTypes(descriptor)).filter(x -> !x.toString().equals("[")).count();
//            final CFGNode callNode = new ICFGCallNode(currentInstructionIndex, opcode, calledMethodName);
//            nodes.put((double) currentInstructionIndex + 0.1, callNode);
////            final ToBeDeleted node = new ToBeDeleted(currentInstructionIndex, currentLineNumber, opcode, owner, null, callSiteMethodName, paramsCount);
//            final CFGNode returnNode = new ICFGReturnNode(currentInstructionIndex, Integer.MIN_VALUE);
//            nodes.put((double) currentInstructionIndex + 0.9, returnNode);
//            crNodes.add((double) currentInstructionIndex);
//        } else {
            final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
            nodes.put((double) currentInstructionIndex, node);
//        }
    }

    private String computeInternalMethodName(String name, String descriptor) {
        logger.debug("computeInternalMethodName");
        for (MethodNode node : classVisitor.classNode.methods) {
            if (node.name.equals(name) && node.desc.equals(descriptor)) {
                return CFGCreator.computeInternalMethodName(node.name, node.desc, node.signature, node.exceptions.toArray(new String[0]));
            }
        }
        return null;
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
        addEntryNode();
        logger.debug(JDFCUtils.prettyPrintMap(nodes));
        logger.debug(JDFCUtils.prettyPrintMultimap(edges));
        setPredecessorSuccessorRelation();

        logger.debug(JDFCUtils.prettyPrintMap(nodes));

        boolean isImpure = false;
        CFG cfg = new CFGImpl(internalMethodName, nodes, edges, localVariableTable, isImpure);
        methodCFGs.put(internalMethodName, cfg);
        classVisitor.classExecutionData.getMethodFirstLine().put(internalMethodName, firstLine);
        classVisitor.classExecutionData.getMethodLastLine().put(internalMethodName, currentLineNumber);
        // Put everything into new MethodData
        classVisitor.classExecutionData.getMethods().get(internalMethodName).setCfg(cfg);
        classVisitor.classExecutionData.getMethods().get(internalMethodName).setFirstLine(firstLine);
        classVisitor.classExecutionData.getMethods().get(internalMethodName).setLastLine(currentLineNumber);
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
        CFGEdgeAnalysisVisitor cfgEdgeAnalysationVisitor =
                new CFGEdgeAnalysisVisitor(methodNode);
        methodNode.accept(cfgEdgeAnalysationVisitor);
        return cfgEdgeAnalysationVisitor.getEdges();
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

    private void addEntryNode() {
        logger.debug("addEntryNode");
        Set<ProgramVariable> parameters = createParamVars();
        final CFGNode firstNode = nodes.get(0.0);
        if (firstNode != null) {
            final CFGNode entryNode = new ICFGEntryNode( parameters, Sets.newLinkedHashSet(), Integer.MIN_VALUE,
                    Integer.MIN_VALUE, Sets.newLinkedHashSet(), Sets.newHashSet(firstNode));
            firstNode.addPredecessor(entryNode);
            nodes.put((double) Integer.MIN_VALUE, entryNode);
            edges.put((double) Integer.MIN_VALUE, 0.0);
        }
    }

    private Set<ProgramVariable> createParamVars() {
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
       return parameters;
    }
}
