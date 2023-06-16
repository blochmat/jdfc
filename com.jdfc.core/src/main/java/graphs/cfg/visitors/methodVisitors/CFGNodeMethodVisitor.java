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
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
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
    private final String owner;
    private final int access;
    private final String name;
    private final String descriptor;

    private final CFGAnalyzerAdapter aa;

    public CFGNodeMethodVisitor(final String owner,
                                final int access,
                                final String name,
                                final String descriptor,
                                final CFGNodeClassVisitor pClassVisitor,
                                final MethodVisitor pMethodVisitor,
                                final MethodNode pMethodNode,
                                final String pInternalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
        this.owner = owner;
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;

        edges = ArrayListMultimap.create();
        nodes = Maps.newTreeMap();
        mData = pClassVisitor.classExecutionData.getMethodByInternalName(internalMethodName);

        this.aa = new CFGAnalyzerAdapter(ASM5, owner, access, name, descriptor, null);

        if (name.equals("max")) {
            int i = 0;
            for (AbstractInsnNode n : pMethodNode.instructions) {
                JDFCUtils.logThis(i + " " + n.getOpcode(), "insns");
                i++;
            }
        }
    }

    // This method is required, because in the instruction of the method node many unvisited FRAME_NEW nodes are
    // included, and we have to update the instruction index or our resulting cfg accordingly
    @Override
    public void checkForFrameNew() {
        while (currentNode.getOpcode() == F_NEW) {
            final CFGNode node = new CFGNode(currentInstructionIndex, F_NEW);
            nodes.put(currentInstructionIndex, node);
            if (currentNode.getNext() == null) {
                break;
            } else {
                updateCurrentNode();
            }
        }
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
//        logger.debug("visitFrame");
        super.visitFrame(type, numLocal, local, numStack, stack);
        this.checkForFrameNew();
        JDFCUtils.logThis(String.valueOf(type), "frame");
        aa.visitFrame(type, numLocal, local, numStack, stack);
//        final CFGNode node = new CFGNode(currentInstructionIndex, getFrameOpcode(type));
//        nodes.put(currentInstructionIndex, node);
    }

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
                throw new IllegalArgumentException("Unknown opcode.");
        }
    }

    @Override
    public void visitInsn(int opcode) {
//        String debug = String.format("visitInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitInsn(opcode);
        this.checkForFrameNew();
        aa.visitInsn(opcode);
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
//        String debug = String.format("visitIntInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitIntInsn(opcode, operand);
        this.checkForFrameNew();
        aa.visitIntInsn(opcode, operand);
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
//        String debug = String.format("visitVarInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitVarInsn(opcode, var);
        this.checkForFrameNew();
        createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
        aa.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
//        String debug = String.format("visitTypeInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitTypeInsn(opcode, type);
        this.checkForFrameNew();
        aa.visitTypeInsn(opcode, type);
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
//        String debug = String.format("visitFieldInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitFieldInsn(opcode, owner, name, descriptor);
        this.checkForFrameNew();
        aa.visitFieldInsn(opcode, owner, name, descriptor);
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // opcode, name of called class, name of called method, desc of called method
//        String debug = String.format("visitMethodInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        this.checkForFrameNew();
        JDFCUtils.logThis(JDFCUtils.prettyPrintArray(aa.stack.toArray()), "stack");
        aa.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        JDFCUtils.logThis(JDFCUtils.prettyPrintArray(aa.stack.toArray()), "stack");
        if (owner.equals(classVisitor.classNode.name)) {
            ASMHelper asmHelper = new ASMHelper();
            String shortInternalName = asmHelper.computeInternalMethodName(name, descriptor, null, null);
            CFGCallNode node = new CFGCallNode(currentInstructionIndex, opcode, owner, shortInternalName, isInterface);
            nodes.put(currentInstructionIndex, node);
            JDFCUtils.logThis(node.toString(), "method_insn");
        } else {
            final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
            nodes.put(currentInstructionIndex, node);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
//        logger.debug("visitInvokeDynamicInsn");
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        this.checkForFrameNew();
        aa.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        final CFGNode node = new CFGNode(currentInstructionIndex, INVOKEDYNAMIC);
        JDFCUtils.logThis(String.format("%s %s %s %s %s", "invokedynamic", name, descriptor, bootstrapMethodHandle, Arrays.toString(bootstrapMethodArguments)), "dynamic_insn");
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
//        String debug = String.format("visitJumpInsn %s %s", JDFCUtils.getOpcode(opcode), label);
//        logger.debug(debug);
        super.visitJumpInsn(opcode, label);
        this.checkForFrameNew();
        aa.visitJumpInsn(opcode, label);
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLdcInsn(Object value) {
//        logger.debug("visitLdcInsn");
        super.visitLdcInsn(value);
        this.checkForFrameNew();
        aa.visitLdcInsn(value);
        final CFGNode node = new CFGNode(currentInstructionIndex, LDC);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
//        logger.debug("visitIincInsn");
        super.visitIincInsn(var, increment);
        this.checkForFrameNew();
        createCFGNodeForIincInsnNode(var, currentInstructionIndex, currentLineNumber);
        aa.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
//        logger.debug("visitTableSwitchInsn");
        super.visitTableSwitchInsn(min, max, dflt, labels);
        this.checkForFrameNew();
        aa.visitTableSwitchInsn(min, max, dflt, labels);
        final CFGNode node = new CFGNode(currentInstructionIndex, TABLESWITCH);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//        logger.debug("visitLookupSwitchInsn");
        super.visitLookupSwitchInsn(dflt, keys, labels);
        this.checkForFrameNew();
        aa.visitLookupSwitchInsn(dflt, keys, labels);
        final CFGNode node = new CFGNode(currentInstructionIndex, LOOKUPSWITCH);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
//        logger.debug("visitMultiANewArrayInsn");
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        this.checkForFrameNew();
        aa.visitMultiANewArrayInsn(descriptor, numDimensions);
        final CFGNode node = new CFGNode(currentInstructionIndex, MULTIANEWARRAY);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitParameter(String name, int access) {
        this.checkForFrameNew();
        super.visitParameter(name, access);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        this.checkForFrameNew();
        return super.visitAnnotationDefault();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        this.checkForFrameNew();
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        this.checkForFrameNew();
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        this.checkForFrameNew();
        super.visitAnnotableParameterCount(parameterCount, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        this.checkForFrameNew();
        return super.visitParameterAnnotation(parameter, descriptor, visible);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        this.checkForFrameNew();
        super.visitAttribute(attribute);
    }

    @Override
    public void visitLabel(Label label) {
        // no check necessary, because no insn exists in methodNode
        super.visitLabel(label);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        this.checkForFrameNew();
        return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        this.checkForFrameNew();
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        this.checkForFrameNew();
        return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        this.checkForFrameNew();
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
        this.checkForFrameNew();
        return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
    }

    @Override
    public void visitEnd() {
//        logger.debug("visitEnd");
        super.visitEnd();
        this.checkForFrameNew();

        this.edges.putAll(createEdges());
        JDFCUtils.logThis(this.internalMethodName + "\n" + JDFCUtils.prettyPrintMap(this.nodes), "nodes");
        JDFCUtils.logThis(this.internalMethodName + "\n" + JDFCUtils.prettyPrintMultimap(this.edges), "edges");
        this.addEntryAndExitNode();
        JDFCUtils.logThis(this.internalMethodName + "\n" + JDFCUtils.prettyPrintMap(this.nodes), "nodes");
        JDFCUtils.logThis(this.internalMethodName + "\n" + JDFCUtils.prettyPrintMultimap(this.edges), "edges");
        this.setPredecessorSuccessorRelation();
        JDFCUtils.logThis(this.internalMethodName + "\n" + JDFCUtils.prettyPrintMap(this.nodes), "nodes");
        JDFCUtils.logThis(this.internalMethodName + "\n" + JDFCUtils.prettyPrintMultimap(this.edges), "edges");
        CFG cfg = new CFGImpl(this.internalMethodName, this.nodes, this.edges);

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
                aa.setPVar(programVariable);
                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), pIndex, opcode);
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                programVariable = getProgramVariableFromLocalVar(varNumber, opcode, pIndex, lineNumber);
                aa.setPVar(programVariable);
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
        aa.setPVar(programVariable);
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
        // TODO: Entry and exit nodes are not correctly connected.
        Set<ProgramVariable> parameters = createParamVars();
        for(ProgramVariable param : parameters) {
            UUID id = UUID.randomUUID();
            this.mData.getProgramVariables().put(id, param);
        }

        // Copy nodes
        NavigableMap<Integer, CFGNode> tempNodes = Maps.newTreeMap();
        tempNodes.putAll(this.nodes);

        // Put entry node
        final CFGNode entryNode =
                new CFGEntryNode(parameters, Sets.newLinkedHashSet(), Sets.newLinkedHashSet(), Sets.newLinkedHashSet());
        this.nodes.put(0, entryNode);

        // Put all other nodes
        for(Map.Entry<Integer, CFGNode> entry : tempNodes.entrySet()) {
            this.nodes.put(entry.getKey()+1, entry.getValue());
        }

        // Put exit node
        final CFGNode exitNode =
                new CFGExitNode(Sets.newLinkedHashSet(), Sets.newLinkedHashSet(), Sets.newHashSet(), Sets.newLinkedHashSet());
        if (this.nodes.get(this.nodes.size() - 1).getOpcode() == F_NEW) {
            this.nodes.put(this.nodes.size() - 1, exitNode);
        } else {
            this.nodes.put(this.nodes.size(), exitNode);
        }

        // Copy edges
        Multimap<Integer, Integer> tempEdges = ArrayListMultimap.create();
        tempEdges.putAll(this.edges);
        this.edges.clear();

        // Put edges
        for(Map.Entry<Integer, Integer> entry : tempEdges.entries()) {
            this.edges.put(entry.getKey()+1, entry.getValue()+1);
        }

        // Put edges for entry and exit node
        if(this.nodes.size() == 2) {
            // only contains entry and exit nodes
            this.edges.put(0, 1);
        } else {
            // Add edge from entry node
            final CFGNode firstNode = this.nodes.get(0);
            if(firstNode == null) {
                throw new RuntimeException("Add entry node failed, because first node was null.");
            }
            this.edges.put(0, 1);

            // Add edges to exit node
            for (Map.Entry<Integer, CFGNode> nodeEntry : this.nodes.entrySet()) {
                if (172 <= nodeEntry.getValue().getOpcode() && nodeEntry.getValue().getOpcode() <= 177) {
                    this.edges.put(nodeEntry.getKey(), this.nodes.size()-1);
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
