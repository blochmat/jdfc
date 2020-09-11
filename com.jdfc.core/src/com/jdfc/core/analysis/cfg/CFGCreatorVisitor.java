package com.jdfc.core.analysis.cfg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
        System.out.println("visitMethod");
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

        if (methodNode != null) {
            return new MethodCFGCreatorVisitor(
                    mv, name, localVariableTableKey, methodCFGs, localVariableTable, parameterTypes, methodNode);
        }

        return null;
    }

    private MethodNode getMethodNode(String pName) {
        System.out.println("getMethodNode");
        for (MethodNode node : classNode.methods) {
            if (node.name.equals(pName)) {
                return node;
            }
        }
        return null;
    }

    private static class MethodCFGCreatorVisitor extends MethodVisitor {

        private final String name;
        private final String internalName;
        private final Map<String, CFG> methodCFGs;
        private final LocalVariableTable localVariableTable;
        private final Multimap<Integer, Integer> edges;
        private final Map<Integer, CFGNode> nodes;
        private final Type[] parameterTypes;

        private final MethodNode methodNode;
        private int currentLineNumber = -1;
        private AbstractInsnNode currentNode = null;
        private int currentInstructionIndex = -1;

        public MethodCFGCreatorVisitor(MethodVisitor pMethodVisitor,
                                       String pName,
                                       String pInternalName,
                                       Map<String, CFG> pMethodCFGs,
                                       LocalVariableTable pLocalVariableTable,
                                       Type[] pParameterTypes,
                                       MethodNode pMethodNode) {
            super(ASM6, pMethodVisitor);
            System.out.println("MethodCFGCreatorVisitor");
            name = pName;
            internalName = pInternalName;
            methodCFGs = pMethodCFGs;
            localVariableTable = pLocalVariableTable;
            parameterTypes = pParameterTypes;
            methodNode = pMethodNode;
            edges = ArrayListMultimap.create();
            nodes = Maps.newLinkedHashMap();
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            System.out.println("visitLineNumber");
            currentLineNumber = line;
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
            System.out.println("visitFrame");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitFrame(type, numLocal, local, numStack, stack);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            System.out.println("visitVarInsn");
            updateCurrentNode();
            createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
            System.out.printf("%s %s\n", opcode, currentInstructionIndex);
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            System.out.println("visitIincInsn");
            updateCurrentNode();
            createCFGNodeForIincInsnNode(var, currentInstructionIndex, currentLineNumber);
            super.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            System.out.println("visitInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            System.out.printf("%s %s\n", opcode, currentInstructionIndex);
            super.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            System.out.println("visitIntInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            System.out.println("visitTypeInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            System.out.println("visitFieldInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            System.out.println("visitMethodInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            System.out.printf("%s %s\n", opcode, currentInstructionIndex);
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            System.out.println("visitInvokeDynamicInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            System.out.println("visitJumpInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            System.out.printf("%s %s\n", opcode, currentInstructionIndex);
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLdcInsn(Object value) {
            System.out.println("visitLdcInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitLdcInsn(value);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            System.out.println("visitTableSwitchInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            System.out.println("visitLookupSwitchInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            System.out.println("visitMultiANewArrayInsn");
            updateCurrentNode();
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
            super.visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        @Override
        public void visitEnd() {
            System.out.println("visitEnd");

            edges.putAll(createEdges());
            setPredecessorSuccessorRelation();
            addDummyStartNode();

            CFG cfg = new CFGImpl(name, nodes, localVariableTable);
            cfg.calculateReachingDefinitions();
            methodCFGs.put(internalName, cfg);
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

        private void createCFGNodeForVarInsnNode(final int opcode, final int varNumber, final int pIndex, final int lineNumber) {
            final ProgramVariable programVariable = getProgramVariable(varNumber, pIndex, lineNumber);
            final CFGNode node;
            switch (opcode) {
                case ISTORE:
                case LSTORE:
                case FSTORE:
                case DSTORE:
                    node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), pIndex);
                    break;
                case ILOAD:
                case LLOAD:
                case FLOAD:
                case DLOAD:
                    node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), pIndex);
                    break;
                default:
                    node = new CFGNode(pIndex);
                    break;
            }
            nodes.put(pIndex, node);
        }

        private void createCFGNodeForIincInsnNode(final int varNumber, final int pIndex, final int pLineNumber) {
            System.out.println("createCFGNodeForIincInsnNode");
            final ProgramVariable programVariable = getProgramVariable(varNumber, pIndex, pLineNumber);
            final CFGNode node =
                    new CFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex);
            nodes.put(pIndex, node);
        }

        private ProgramVariable getProgramVariable(int varNumber, final int pIndex, final int pLineNumber) {
            final String varName = getVariableNameFromLocalVariablesTable(varNumber);
            final String varType = getVariableTypeFromLocalVariablesTable(varNumber);
            return ProgramVariable.create(varName, varType, pIndex, pLineNumber);
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
            System.out.println("createEdges");
            CFGEdgeAnalyzationVisitor cfgEdgeAnalysationVisitor =
                    new CFGEdgeAnalyzationVisitor(name, methodNode);
            methodNode.accept(cfgEdgeAnalysationVisitor);
            return cfgEdgeAnalysationVisitor.getEdges();
        }

        private void setPredecessorSuccessorRelation() {
            System.out.println("setPredecessorSuccessorRelation");
            for (Map.Entry<Integer, Integer> edge : edges.entries()) {
                final CFGNode first = nodes.get(edge.getKey());
                final CFGNode second = nodes.get(edge.getValue());
                System.out.printf("FIRST: %s\n", first);
                System.out.printf("SECOND: %s\n", second);
                first.addSuccessor(second);
                second.addPredecessor(first);
            }
        }

        private void addDummyStartNode() {
            System.out.println("addDummyStartNode");
            final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();
            for (int i = 0; i < parameterTypes.length; i++) {
                final Optional<LocalVariable> parameterVariable = localVariableTable.getEntry(i + 1);
                if (parameterVariable.isPresent()) {
                    final LocalVariable localVariable = parameterVariable.get();
                    final ProgramVariable variable =
                            ProgramVariable.create(
                                    localVariable.getName(), localVariable.getDescriptor(), Integer.MIN_VALUE, Integer.MIN_VALUE);
                    parameters.add(variable);
                }
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

