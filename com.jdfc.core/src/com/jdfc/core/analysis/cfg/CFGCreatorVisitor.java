package com.jdfc.core.analysis.cfg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.DLOAD;

public class CFGCreatorVisitor extends ClassVisitor {

    private final Map<String, CFG> methodCFGs;
    private final Map<String, LocalVariableTable> localVariableTables;
    private final ClassNode classNode;
    final String jacocoMethodName = "$jacoco";

    public CFGCreatorVisitor(Map<String, CFG> pMethodCFGs,
                             Map<String, LocalVariableTable> pLocalVariableTables,
                             ClassNode pClassNode) {
        super(Opcodes.ASM6);
        this.methodCFGs = pMethodCFGs;
        this.localVariableTables = pLocalVariableTables;
        classNode = pClassNode;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//        System.out.println("visitMethod");
        MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(access, name, desc, signature, exceptions);
        } else {
            mv = null;
        }
        final String localVariableTableKey = CFGCreator.computeInternalMethodName(name, desc, signature, exceptions);
        final LocalVariableTable localVariableTable = localVariableTables.get(localVariableTableKey);
        final Type[] parameterTypes = Type.getArgumentTypes(desc);
        final MethodNode methodNode = getMethodNode(name);

        if (methodNode != null && !isJacocoInstrumentation(name)) {
            return new MethodCFGCreatorVisitor(
                    this, mv, name, localVariableTableKey, methodCFGs, localVariableTable, parameterTypes, methodNode);
        }

        return mv;
    }

    private MethodNode getMethodNode(String pName) {
//        System.out.println("getMethodNode");
        for (MethodNode node : classNode.methods) {
            if (node.name.equals(pName)) {
                return node;
            }
        }
        return null;
    }

    private boolean isJacocoInstrumentation(String pString) {
        return pString.contains(jacocoMethodName);
    }

    private static class MethodCFGCreatorVisitor extends MethodVisitor {

        private final CFGCreatorVisitor classVisitor;
        private final String methodName;
        private final String internalMethodName;
        private final Map<String, CFG> methodCFGs;
        private final LocalVariableTable localVariableTable;
        private final Multimap<Integer, Integer> edges;
        private final Map<Integer, CFGNode> nodes;
        private final Type[] parameterTypes;

        private final MethodNode methodNode;
        private int currentLineNumber = -1;
        private AbstractInsnNode currentNode = null;
        private int currentInstructionIndex = -1;
        private int firstLine = -1;

        public MethodCFGCreatorVisitor(CFGCreatorVisitor pClassVisitor,
                                       MethodVisitor pMethodVisitor,
                                       String pMethodName,
                                       String pInternalMethodName,
                                       Map<String, CFG> pMethodCFGs,
                                       LocalVariableTable pLocalVariableTable,
                                       Type[] pParameterTypes,
                                       MethodNode pMethodNode) {
            super(ASM6, pMethodVisitor);
            classVisitor = pClassVisitor;
            methodName = pMethodName;
            internalMethodName = pInternalMethodName;
            methodCFGs = pMethodCFGs;
            localVariableTable = pLocalVariableTable;
            parameterTypes = pParameterTypes;
            methodNode = pMethodNode;
            edges = ArrayListMultimap.create();
            nodes = Maps.newLinkedHashMap();
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (firstLine == -1) {
                firstLine += line;
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
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
//            System.out.printf("%s %s\n", opcode, currentInstructionIndex);
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
            ClassExecutionData classExecutionData =
                    (ClassExecutionData) CoverageDataStore.getInstance()
                            .findClassDataNode(classVisitor.classNode.name).getData();

            edges.putAll(createEdges());
            setPredecessorSuccessorRelation();

            // Add all instance variables to dummy start node as they are defined for all methods
            addDummyStartNode(classExecutionData.getInstanceVariables());

            CFG cfg = new CFGImpl(methodName, nodes, localVariableTable);
            cfg.calculateReachingDefinitions();
            methodCFGs.put(internalMethodName, cfg);
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

        private void addDummyStartNode(Set<InstanceVariable> pInstanceVariables) {
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

            for(InstanceVariable instanceVariable : pInstanceVariables) {
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
            final CFGNode dummyStartNode =
                    new CFGNode(
                            parameters,
                            Sets.newLinkedHashSet(),
                            Integer.MIN_VALUE,
                            Sets.newLinkedHashSet(),
                            Sets.newHashSet(firstNode));
            firstNode.addPredecessor(dummyStartNode);
            nodes.put(Integer.MIN_VALUE, dummyStartNode);
        }
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

