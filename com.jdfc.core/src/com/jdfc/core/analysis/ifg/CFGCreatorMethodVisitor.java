package com.jdfc.core.analysis.ifg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.data.ClassExecutionData;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

class CFGCreatorMethodVisitor extends MethodVisitor {

    private final String className;
    private final ClassExecutionData classExecutionData;
    private final MethodNode methodNode;
    private final String methodName;
    private final String internalMethodName;
    private final Map<String, CFG> methodCFGs;
    private final LocalVariableTable localVariableTable;
    private final Type[] parameterTypes;
    private final Multimap<Integer, Integer> edges;
    private final NavigableMap<Integer, CFGNode> nodes;

    private int currentLineNumber = -1;
    private AbstractInsnNode currentNode = null;
    private int currentInstructionIndex = -1;
    private int firstLine = -1;
    private boolean isImpure = false;

    public CFGCreatorMethodVisitor(final String pClassName,
                                   final ClassExecutionData pClassExecutionData,
                                   final MethodVisitor pMethodVisitor,
                                   final MethodNode pMethodNode,
                                   final String pMethodName,
                                   final String pInternalMethodName,
                                   final Map<String, CFG> pMethodCFGs,
                                   final LocalVariableTable pLocalVariableTable,
                                   final Type[] pParameterTypes
    ) {
        super(ASM6, pMethodVisitor);
        className = pClassName;
        classExecutionData = pClassExecutionData;
        methodNode = pMethodNode;
        methodName = pMethodName;
        internalMethodName = pInternalMethodName;
        methodCFGs = pMethodCFGs;
        localVariableTable = pLocalVariableTable;
        parameterTypes = pParameterTypes;
        edges = ArrayListMultimap.create();
        nodes = Maps.newTreeMap();
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (firstLine == -1) {
            if (methodName.equals("<init>")) {
                firstLine = line;
            } else {
                firstLine += line;
            }
        }
//            System.out.println("visitLineNumber");
        currentLineNumber = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
//            System.out.println("visitFrame");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
        super.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
//            System.out.println("visitVarInsn");
        updateCurrentNode();
        createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
//            System.out.printf("%s %s\n", opcode, currentInstructionIndex);
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
//            System.out.println("visitIincInsn");
        updateCurrentNode();
        createCFGNodeForIincInsnNode(var, currentInstructionIndex, currentLineNumber);
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitInsn(int opcode) {
//            System.out.println("visitInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
//            System.out.printf("%s %s\n", opcode, currentInstructionIndex);
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
//            System.out.println("visitIntInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
//            System.out.println("visitTypeInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
//            System.out.println("visitFieldInsn");
        updateCurrentNode();
        createCFGNodeForFieldInsnNode(opcode, owner, name, descriptor);
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
//            System.out.println("visitMethodInsn");

        // TODO: Clarify interprocedural behavior and implement it
        // TODO: Maybe just delete
        updateCurrentNode();
        if (owner.equals(className)) {
            String methodNameDesc = name.concat(": " + descriptor);
            List<String> allParams =
                    Arrays.stream(descriptor.split("[(;)]")).filter(x -> !x.equals("")).collect(Collectors.toList());
            String returnParam = allParams.get(allParams.size() - 1);
            allParams.remove(allParams.size() - 1);
            List<String> passedParams = new ArrayList<>();
            for(String param : allParams) {
                if(!param.contains("/")) {
                    passedParams.addAll(Arrays.asList(param.split("")));
                } else {
                    passedParams.add(param);
                }
            }
            final IFGNode node = new IFGNode(currentInstructionIndex, methodNameDesc, passedParams.size());
            nodes.put(currentInstructionIndex, node);

//            List<AbstractInsnNode> varInsnNodes = new ArrayList<>();
//            // get passed parameters by instruction index from method node
//            for (int i = 1; i <= passedParams.size(); i++) {
//                if (currentInstructionIndex - i >= 0) {
//                    AbstractInsnNode instruction = methodNode.instructions.get(currentInstructionIndex - i);
//                    switch (instruction.getOpcode()) {
//                        case Opcodes.LDC:
//                            // constants and final variables
//                            // not decidable yet as we need the value of a variable to decide if its actually a variable
//                            // and no local constant
//                            break;
//                        case Opcodes.ILOAD:
//                        case Opcodes.DLOAD:
//                        case Opcodes.FLOAD:
//                        case Opcodes.LLOAD:
//                        case Opcodes.ALOAD:
//                            // Local Variables
//                            // usage is already tracked and there can not be a new definition because in the scope
//                            // of the other method the variable is aliased and therefore not tracked
//
//                            break;
//                        case Opcodes.GETFIELD:
//                            // Instance Variables
//                            FieldInsnNode getFieldInsnNode = (FieldInsnNode) instruction;
//                            break;
//                    }
//                }
//            }
            // create new programVariables or safe as use or definition in corresponding method node
            // either new define them after method call or make purity analysis after method call
        } else {
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
//            System.out.println("visitInvokeDynamicInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
//            System.out.println("visitJumpInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
//            System.out.printf("%s %s\n", opcode, currentInstructionIndex);
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
//            System.out.println("visitLdcInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
        super.visitLdcInsn(value);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
//            System.out.println("visitTableSwitchInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//            System.out.println("visitLookupSwitchInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
//            System.out.println("visitMultiANewArrayInsn");
        updateCurrentNode();
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitEnd() {
//            System.out.println("visitEnd");

        edges.putAll(createEdges());
        setPredecessorSuccessorRelation();

        // Add all instance variables to dummy start node as they are defined for all methods
        addEntryNode(classExecutionData.getInstanceVariables());
        addExitNode();

        CFG cfg = new CFGImpl(methodName, nodes, localVariableTable, isImpure);
        cfg.calculateReachingDefinitions();
        methodCFGs.put(internalMethodName, cfg);

        // TODO: Recompute methodRangeMap
        int lastLine = currentLineNumber;
        classExecutionData.getMethodRangeMap().put(internalMethodName, new Pair<>(firstLine, lastLine));
        super.visitEnd();
    }

    private void updateCurrentNode() {
        if (currentNode == null) {
            currentNode = methodNode.instructions.getFirst();
        } else {
            currentNode = currentNode.getNext();
        }
        currentInstructionIndex = methodNode.instructions.indexOf(currentNode);
    }

    private void createCFGNodeForFieldInsnNode(final int pOpcode, String pOwner, String pName, String pDescriptor) {
        final ProgramVariable programVariable =
                ProgramVariable.create(pOwner, pName, pDescriptor, currentInstructionIndex, currentLineNumber);
        final CFGNode node;
        switch (pOpcode) {
            case GETFIELD:
                node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), currentInstructionIndex);
                break;
            case PUTFIELD:
                if (pOwner.equals(className)) {
                    isImpure = true;
                }

                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), currentInstructionIndex);
                break;
            default:
                node = new CFGNode(currentInstructionIndex);
                break;
        }
        nodes.put(currentInstructionIndex, node);
    }

    private void createCFGNodeForVarInsnNode(final int opcode, final int varNumber, final int pIndex, final int lineNumber) {
        final ProgramVariable programVariable = getProgramVariable(varNumber, pIndex, lineNumber);
        final CFGNode node;
        switch (opcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), pIndex);
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), pIndex);
                break;
            default:
                node = new CFGNode(pIndex);
                break;
        }
        nodes.put(pIndex, node);
    }

    private void createCFGNodeForIincInsnNode(final int varNumber, final int pIndex, final int pLineNumber) {
//            System.out.println("createCFGNodeForIincInsnNode");
        final ProgramVariable programVariable = getProgramVariable(varNumber, pIndex, pLineNumber);
        final CFGNode node =
                new CFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex);
        nodes.put(pIndex, node);
    }

    private ProgramVariable getProgramVariable(int varNumber, final int pIndex, final int pLineNumber) {
        final String varName = getVariableNameFromLocalVariablesTable(varNumber);
        final String varType = getVariableTypeFromLocalVariablesTable(varNumber);
        return ProgramVariable.create(null, varName, varType, pIndex, pLineNumber);
    }

    private String getVariableNameFromLocalVariablesTable(final int pVarNumber) {
        final Optional<LocalVariable> localVariable = localVariableTable.getEntry(pVarNumber);
        if (localVariable.isPresent()) {
            return localVariable.get().getName();
        } else {
            return String.valueOf(pVarNumber);
        }
    }

    private String getVariableTypeFromLocalVariablesTable(final int pVarNumber) {
        final Optional<LocalVariable> localVariable = localVariableTable.getEntry(pVarNumber);
        if (localVariable.isPresent()) {
            return localVariable.get().getDescriptor();
        } else {
            return "UNKNOWN";
        }
    }

    private Multimap<Integer, Integer> createEdges() {
//            System.out.println("createEdges");
        CFGEdgeAnalyzationVisitor cfgEdgeAnalysationVisitor =
                new CFGEdgeAnalyzationVisitor(methodName, methodNode);
        methodNode.accept(cfgEdgeAnalysationVisitor);
        return cfgEdgeAnalysationVisitor.getEdges();
    }

    private void setPredecessorSuccessorRelation() {
//            System.out.println("setPredecessorSuccessorRelation");
        for (Map.Entry<Integer, Integer> edge : edges.entries()) {
            final CFGNode first = nodes.get(edge.getKey());
            final CFGNode second = nodes.get(edge.getValue());
//                System.out.printf("FIRST: %s\n", first);
//                System.out.printf("SECOND: %s\n", second);
            first.addSuccessor(second);
            second.addPredecessor(first);
        }
    }

    private void addEntryNode(Set<InstanceVariable> pInstanceVariables) {
//            System.out.println("addDummyStartNode");
        final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();
        for (int i = 0; i < parameterTypes.length; i++) {
            final Optional<LocalVariable> parameterVariable = localVariableTable.getEntry(i + 1);
            if (parameterVariable.isPresent()) {
                final LocalVariable localVariable = parameterVariable.get();
                final ProgramVariable variable =
                        ProgramVariable.create(null,
                                localVariable.getName(),
                                localVariable.getDescriptor(),
                                Integer.MIN_VALUE,
                                firstLine);
                parameters.add(variable);
            }
        }

        for (InstanceVariable instanceVariable : pInstanceVariables) {
            final ProgramVariable variable =
                    ProgramVariable.create(
                            instanceVariable.getOwner(),
                            instanceVariable.getName(),
                            instanceVariable.getDescriptor(),
                            Integer.MIN_VALUE,
                            instanceVariable.getLineNumber());
            parameters.add(variable);
        }

        final CFGNode firstNode = nodes.get(0);
        final CFGNode entryNode =
                new CFGNode(
                        parameters,
                        Sets.newLinkedHashSet(),
                        Integer.MIN_VALUE,
                        Sets.newLinkedHashSet(),
                        Sets.newHashSet(firstNode));
        firstNode.addPredecessor(entryNode);
        nodes.put(Integer.MIN_VALUE, entryNode);
    }

    private void addExitNode() {
//        System.out.println("addExitNode");
        // TODO: You may add some parameters here
        final CFGNode lastNode = nodes.get(currentInstructionIndex);
        final CFGNode exitNode = new CFGNode(Integer.MAX_VALUE);
        lastNode.addSuccessor(exitNode);
        nodes.put(Integer.MAX_VALUE, exitNode);
    }

    private static class CFGEdgeAnalyzationVisitor extends MethodVisitor {

        private final String owner;
        private final MethodNode methodNode;
        private final Multimap<Integer, Integer> edges;

        CFGEdgeAnalyzationVisitor(final String pOwner, final MethodNode pMethodNode) {
            super(ASM6);
            owner = pOwner;
            methodNode = pMethodNode;
            edges = ArrayListMultimap.create();
        }

        @Override
        public void visitEnd() {
            try {
                CFGEdgeAnalyzer cfgEdgeAnalyzer = new CFGEdgeAnalyzer();
                cfgEdgeAnalyzer.analyze(owner, methodNode);
                edges.putAll(cfgEdgeAnalyzer.getEdges());
            } catch (AnalyzerException e) {
                e.printStackTrace();
            }
        }

        Multimap<Integer, Integer> getEdges() {
            return edges;
        }
    }

    private static class CFGEdgeAnalyzer extends Analyzer<SourceValue> {

        private final Multimap<Integer, Integer> edges;

        CFGEdgeAnalyzer() {
            super(new SourceInterpreter());
            edges = ArrayListMultimap.create();
        }

        Multimap<Integer, Integer> getEdges() {
            return edges;
        }

        @Override
        protected void newControlFlowEdge(int insnIndex, int successorIndex) {
            if (!edges.containsKey(insnIndex) || !edges.containsValue(successorIndex)) {
                edges.put(insnIndex, successorIndex);
            }
            super.newControlFlowEdge(insnIndex, successorIndex);
        }

        @Override
        protected boolean newControlFlowExceptionEdge(int insnIndex, int successorIndex) {
            if (!edges.containsKey(insnIndex) || !edges.containsValue(successorIndex)) {
                edges.put(insnIndex, successorIndex);
            }
            return super.newControlFlowExceptionEdge(insnIndex, successorIndex);
        }
    }
}
