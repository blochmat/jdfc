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
import graphs.cfg.nodes.CFGCallNode;
import graphs.cfg.nodes.CFGEntryNode;
import graphs.cfg.nodes.CFGExitNode;
import graphs.cfg.nodes.CFGNode;
import graphs.cfg.visitors.classVisitors.CFGNodeClassVisitor;
import instr.methodVisitors.JDFCMethodVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import utils.ASMHelper;
import utils.JDFCUtils;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

@Slf4j
public class CFGNodeMethodVisitor extends JDFCMethodVisitor {
    private final Multimap<Integer, Integer> edges;
    private final NavigableMap<Integer, CFGNode> nodes;
    private final MethodData mData;
    private CFGAnalyzerAdapter aa;

    public CFGNodeMethodVisitor(final CFGNodeClassVisitor pClassVisitor,
                                final MethodVisitor pMethodVisitor,
                                final MethodNode pMethodNode,
                                final String pInternalMethodName,
                                final CFGAnalyzerAdapter aa) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
        edges = ArrayListMultimap.create();
        nodes = Maps.newTreeMap();
        mData = pClassVisitor.classExecutionData.getMethodByInternalName(internalMethodName);
        this.aa = aa;
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
//        logger.debug("visitFrame");
        super.visitFrame(type, numLocal, local, numStack, stack);
        aa.visitFrame(type, numLocal, local, numStack, stack);
        final CFGNode node = new CFGNode(currentInstructionIndex, getFrameOpcode(type));
        nodes.put(currentInstructionIndex, node);
    }


    @Override
    public void visitInsn(int opcode) {
//        String debug = String.format("visitInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitInsn(opcode);
        aa.visitInsn(opcode);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
//        String debug = String.format("visitIntInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitIntInsn(opcode, operand);
        aa.visitIntInsn(opcode, operand);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
//        String debug = String.format("visitVarInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitVarInsn(opcode, var);
        createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
        aa.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
//        String debug = String.format("visitTypeInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitTypeInsn(opcode, type);
        aa.visitTypeInsn(opcode, type);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
//        String debug = String.format("visitFieldInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitFieldInsn(opcode, owner, name, descriptor);
        aa.visitFieldInsn(opcode, owner, name, descriptor);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // opcode, name of called class, name of called method, desc of called method
//        String debug = String.format("visitMethodInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        aa.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
//        checkForF_NEW();
        if (owner.equals(classVisitor.classNode.name)) {
            ASMHelper asmHelper = new ASMHelper();
            String shortInternalName = asmHelper.computeInternalMethodName(name, descriptor, null, null);
            JDFCUtils.logThis(JDFCUtils.prettyPrintArray(aa.getPopList().toArray()), "popped");
            JDFCUtils.logThis(JDFCUtils.prettyPrintArray(aa.stack.toArray()), "stack");
            CFGCallNode node = new CFGCallNode(currentInstructionIndex, opcode, owner, shortInternalName, isInterface);
            nodes.put(currentInstructionIndex, node);
        } else {
            final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
            nodes.put(currentInstructionIndex, node);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
//        logger.debug("visitInvokeDynamicInsn");
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        aa.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, INVOKEDYNAMIC);
        JDFCUtils.logThis(String.format("%s %s %s %s %s", "invokedynamic", name, descriptor, bootstrapMethodHandle, Arrays.toString(bootstrapMethodArguments)), "dynamic_insn");
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
//        String debug = String.format("visitJumpInsn %s %s", JDFCUtils.getOpcode(opcode), label);
//        logger.debug(debug);
        super.visitJumpInsn(opcode, label);
        aa.visitJumpInsn(opcode, label);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLdcInsn(Object value) {
//        logger.debug("visitLdcInsn");
        super.visitLdcInsn(value);
        aa.visitLdcInsn(value);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, LDC);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
//        logger.debug("visitIincInsn");
        super.visitIincInsn(var, increment);
        createCFGNodeForIincInsnNode(var, currentInstructionIndex, currentLineNumber);
        aa.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
//        logger.debug("visitTableSwitchInsn");
        super.visitTableSwitchInsn(min, max, dflt, labels);
        aa.visitTableSwitchInsn(min, max, dflt, labels);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, TABLESWITCH);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//        logger.debug("visitLookupSwitchInsn");
        super.visitLookupSwitchInsn(dflt, keys, labels);
        aa.visitLookupSwitchInsn(dflt, keys, labels);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, LOOKUPSWITCH);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
//        logger.debug("visitMultiANewArrayInsn");
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        aa.visitMultiANewArrayInsn(descriptor, numDimensions);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(currentInstructionIndex, MULTIANEWARRAY);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitEnd() {
//        logger.debug("visitEnd");
        super.visitEnd();

        edges.putAll(createEdges());
        this.addEntryAndExitNode();
        this.setPredecessorSuccessorRelation();
        CFG cfg = new CFGImpl(internalMethodName, nodes, edges);

//        logger.debug(internalMethodName);
//        logger.debug(JDFCUtils.prettyPrintMultimap(edges));

        if (!internalMethodName.contains("<init>") && !internalMethodName.contains("<clinit>")) {
            MethodData mData = classVisitor.classExecutionData.getMethodByInternalName(internalMethodName);
            mData.setCfg(cfg);
            mData.getCfg().calculateReachingDefinitions();
            mData.calculateDefUsePairs();
        } else {
            // TODO: <init>: ()V is not in methods
        }
    }

    // ---------------------------------------------- Helper Methods ---------------------------------------------------

    private int getFrameOpcode(int type) {
//        logger.debug("getFrameOpcode");
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
    public void checkForF_NEW() {
//        logger.debug("checkForF_NEW");
        if (currentNode.getOpcode() == F_NEW) {
            final CFGNode node = new CFGNode(currentInstructionIndex, F_NEW);
            nodes.put(currentInstructionIndex, node);
            updateCurrentNode();
        }
    }

    private ProgramVariable getProgramVariableFromLocalVar(final int varNumber,
                                                           final int pOpcode,
                                                           final int pIndex,
                                                           final int pLineNumber) {
//        logger.debug(String.format("getProgramVariableFromLocalVar(%d, %d, %d, %d)", varNumber, pOpcode, pIndex, pLineNumber));
        final String varName = getLocalVarName(varNumber);
        final String varType = getLocalVarType(varNumber);
        final boolean isDefinition = isDefinition(pOpcode);
        ProgramVariable var = new ProgramVariable(null, varName, varType, pIndex, pLineNumber, isDefinition, false);
        UUID id = UUID.randomUUID();
        mData.getProgramVariables().put(id, var);
        aa.setPVar(var);
        return var;
    }

    private String getLocalVarName(final int pVarNumber) {
//        logger.debug(String.format("getLocalVarName(%d)", pVarNumber));
        final LocalVariable localVariable = mData.getLocalVariableTable().get(pVarNumber);
        if (localVariable != null) {
            return localVariable.getName();
        } else {
            return String.valueOf(pVarNumber);
        }
    }

    private String getLocalVarType(final int pVarNumber) {
//        logger.debug(String.format("getLocalVarType(%d)", pVarNumber));
        final LocalVariable localVariable = mData.getLocalVariableTable().get(pVarNumber);
        if (localVariable != null) {
            return localVariable.getDescriptor();
        } else {
            return "UNKNOWN";
        }
    }

    private void createCFGNodeForVarInsnNode(final int opcode, final int varNumber, final int pIndex, final int lineNumber) {
//        logger.debug("createCFGNodeForVarInsnNode");
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
        nodes.put(pIndex, node);
    }

    private void createCFGNodeForIincInsnNode(final int varNumber, final int pIndex, final int pLineNumber) {
//        logger.debug("createCFGNodeForIincInsnNode");
        final ProgramVariable programVariable = getProgramVariableFromLocalVar(varNumber, ISTORE, pIndex, pLineNumber);
        final CFGNode node =
                new CFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex, IINC);
        nodes.put(pIndex, node);
    }

    private Multimap<Integer, Integer> createEdges() {
//        logger.debug("createEdges");
        CFGEdgeAnalysisVisitor cfgEdgeAnalysisVisitor =
                new CFGEdgeAnalysisVisitor(methodNode);
        methodNode.accept(cfgEdgeAnalysisVisitor);
        return cfgEdgeAnalysisVisitor.getEdges();
    }

    private void setPredecessorSuccessorRelation() {
//        logger.debug("setPredecessorSuccessorRelation");
        for (Map.Entry<Integer, Integer> edge : edges.entries()) {
            final CFGNode first = nodes.get(edge.getKey());
            final CFGNode second = nodes.get(edge.getValue());
            first.addSuccessor(second);
            second.addPredecessor(first);
        }
    }

    private void addEntryAndExitNode() {
//        logger.debug("addEntryAndExitNode");
        Set<ProgramVariable> parameters = createParamVars();
        for(ProgramVariable param : parameters) {
            UUID id = UUID.randomUUID();
            mData.getProgramVariables().put(id, param);
        }

        // Copy nodes
        NavigableMap<Integer, CFGNode> tempNodes = Maps.newTreeMap();
        tempNodes.putAll(this.nodes);

        // Put entry node
        final CFGNode entryNode =
                new CFGEntryNode(parameters, Sets.newLinkedHashSet(), Sets.newLinkedHashSet(), Sets.newLinkedHashSet());
        nodes.put(0, entryNode);

        // Put all other nodes
        for(Map.Entry<Integer, CFGNode> entry : tempNodes.entrySet()) {
            nodes.put(entry.getKey()+1, entry.getValue());
        }

        // Put exit node
        final CFGNode exitNode =
                new CFGExitNode(Sets.newLinkedHashSet(), Sets.newLinkedHashSet(), Sets.newHashSet(), Sets.newLinkedHashSet());
        nodes.put(nodes.size(), exitNode);

        // Copy edges
        Multimap<Integer, Integer> tempEdges = ArrayListMultimap.create();
        tempEdges.putAll(this.edges);
        this.edges.clear();

        // Put edges
        for(Map.Entry<Integer, Integer> entry : tempEdges.entries()) {
            edges.put(entry.getKey()+1, entry.getValue()+1);
        }

        // Put edges for entry and exit node
        if(nodes.size() == 2) {
            // only contains entry and exit nodes
            edges.put(0, 1);
        } else {
            // Add edge from entry node
            final CFGNode firstNode = nodes.get(0);
            if(firstNode == null) {
                throw new RuntimeException("Add entry node failed, because first node was null.");
            }
            edges.put(0, 1);

            // Add edges to exit node
            for (Map.Entry<Integer, CFGNode> nodeEntry : nodes.entrySet()) {
                if (172 <= nodeEntry.getValue().getOpcode() && nodeEntry.getValue().getOpcode() <= 177) {
                    edges.put(nodeEntry.getKey(), nodes.size()-1);
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
