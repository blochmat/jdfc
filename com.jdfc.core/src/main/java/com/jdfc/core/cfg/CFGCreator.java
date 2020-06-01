package main.java.com.jdfc.core.cfg;

import static org.objectweb.asm.Opcodes.ASM6;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

/** Creates {@link CFG}s for each method of a class file. */
public class CFGCreator {

    private CFGCreator() {}

    /**
     * Creates the {@link CFG}s for each method of a class and returns a map of method name to {@link
     * CFG}.
     *
     * <p>The key of the map is in the format
     *
     * <pre>
     *   name: descriptor[; signature][; exceptions]
     * </pre>
     *
     * Hence, a default constructor will have the key <code>&lt;init&gt;: ()V</code> and a method
     * "foo" that takes an int and a String and returns a double array will have the key <code> foo:
     * (ILjava/lang/String;)[D</code>.
     *
     * <p>This method is the only method to start the generation of {@link CFG}s. The full creation
     * process of the graphs is then done internally and only the final graphs will be given back to
     * the user.
     *
     * @param pClassReader The class reader instance on the class
     * @param pClassNode An empty class node to start the analysis with
     * @return A map of method name and {@link CFG}
     */
    public static Map<String, CFG> createCFGsForClass(
            final ClassReader pClassReader, final ClassNode pClassNode) {
        Preconditions.checkNotNull(
                pClassReader, "We need a non-null class reader to generate CFGs from.");
        Preconditions.checkNotNull(pClassNode, "We need a non-null class node to generate CFGs from.");

        final CFGLocalVariableTableVisitor localVariableTableVisitor =
                new CFGLocalVariableTableVisitor();
        pClassReader.accept(localVariableTableVisitor, 0);
        final Map<String, LocalVariableTable> localVariableTables =
                localVariableTableVisitor.getLocalVariables();

        pClassReader.accept(pClassNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        final Map<String, CFG> methodCFGs = Maps.newLinkedHashMap();
        for (MethodNode methodNode : pClassNode.methods) {
            final String localVariableTableKey = computeInternalMethodName(methodNode);
            final LocalVariableTable localVariableTable = localVariableTables.get(localVariableTableKey);
            final Type[] parameterTypes = Type.getArgumentTypes(methodNode.desc);
            final MethodCFGCreator methodCFGCreator =
                    new MethodCFGCreator(methodNode, localVariableTable, parameterTypes);
            final CFG cfg = methodCFGCreator.create();
            cfg.calculateReachingDefinitions();
            methodCFGs.put(localVariableTableKey, cfg);
        }
        return Collections.unmodifiableMap(methodCFGs);
    }

    /**
     * Computes the internal method name representation that is used, for example, in the map emitted
     * by {@link CFGCreator#createCFGsForClass(ClassReader, ClassNode)}.
     *
     * @param pMethodNode The method node to get the information from
     * @return The internal method name representation
     */
    public static String computeInternalMethodName(final MethodNode pMethodNode) {
        final String methodName = pMethodNode.name;
        final String descriptor = pMethodNode.desc;
        final String signature = pMethodNode.signature;
        final String[] exceptions;
        if (pMethodNode.exceptions.size() > 0) {
            exceptions = (String[]) pMethodNode.exceptions.toArray(new String[0]);
        } else {
            exceptions = null;
        }
        return computeInternalMethodName(methodName, descriptor, signature, exceptions);
    }

    /**
     * Computes the internal method name representation that is used, for example, in the map emitted
     * by {@link CFGCreator#createCFGsForClass(ClassReader, ClassNode)}.
     *
     * @param pMethodName The name of the method
     * @param pDescriptor The method's descriptor
     * @param pSignature The method's signature
     * @param pExceptions An array of exceptions thrown by the method
     * @return The internal method name representation
     */
    public static String computeInternalMethodName(
            final String pMethodName,
            final String pDescriptor,
            final String pSignature,
            final String[] pExceptions) {
        final StringBuilder result = new StringBuilder();
        result.append(pMethodName);
        result.append(": ");
        result.append(pDescriptor);
        if (pSignature != null) {
            result.append("; ").append(pSignature);
        }
        if (pExceptions != null) {
            result.append("; ").append(Arrays.toString(pExceptions));
        }
        return result.toString();
    }

    private static class MethodCFGCreator {

        private final String methodName;
        private final MethodNode methodNode;
        private final LocalVariableTable localVariableTable;
        private final Multimap<Integer, Integer> edges;
        private final Map<Integer, CFGNode> nodes;
        private final Type[] parameterTypes;

        MethodCFGCreator(
                final MethodNode pMethodNode,
                final LocalVariableTable pLocalVariableTable,
                final Type[] pParameterTypes) {
            methodName = pMethodNode.name;
            methodNode = pMethodNode;
            localVariableTable = pLocalVariableTable;
            edges = ArrayListMultimap.create();
            nodes = Maps.newLinkedHashMap();
            parameterTypes = pParameterTypes;
        }

        CFG create() {
            final InsnList instructions = methodNode.instructions;
            final ListIterator<AbstractInsnNode> instructionIterator = instructions.iterator();
            while (instructionIterator.hasNext()) {
                final AbstractInsnNode node = instructionIterator.next();
                createCFGNode(node, instructions.indexOf(node));
            }
            edges.putAll(createEdges());
            setPredecessorSuccessorRelation();
            addDummyStartNode();
            return new CFGImpl(methodName, nodes, localVariableTable);
        }

        private void createCFGNode(final AbstractInsnNode pNode, final int pIndex) {
            if (pNode instanceof VarInsnNode) {
                createCFGNodeForVarInsnNode((VarInsnNode) pNode, pIndex);
            } else if (pNode instanceof IincInsnNode) {
                createCFGNodeForIincInsnNode((IincInsnNode) pNode, pIndex);
            } else {
                final CFGNode node = new CFGNode(pIndex);
                nodes.put(pIndex, node);
            }
        }

        private void createCFGNodeForVarInsnNode(final VarInsnNode pNode, final int pIndex) {
            final int varNumber = pNode.var;
            final ProgramVariable programVariable = getProgramVariable(varNumber, pIndex);
            final CFGNode node;
            switch (pNode.getOpcode()) {
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

        private ProgramVariable getProgramVariable(int varNumber, final int pInstructionIndex) {
            final String varName = getVariableNameFromLocalVariablesTable(varNumber);
            final String varType = getVariableTypeFromLocalVariablesTable(varNumber);
            return ProgramVariable.create(varName, varType, pInstructionIndex);
        }

        private void createCFGNodeForIincInsnNode(final IincInsnNode pNode, final int pIndex) {
            final int varNumber = pNode.var;
            final ProgramVariable programVariable = getProgramVariable(varNumber, pIndex);
            final CFGNode node =
                    new CFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex);
            nodes.put(pIndex, node);
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

        private void setPredecessorSuccessorRelation() {
            for (Entry<Integer, Integer> edge : edges.entries()) {
                final CFGNode first = nodes.get(edge.getKey());
                final CFGNode second = nodes.get(edge.getValue());

                first.addSuccessor(second);
                second.addPredecessor(first);
            }
        }

        private Multimap<Integer, Integer> createEdges() {
            CFGEdgeAnalyzationVisitor cfgEdgeAnalysationVisitor =
                    new CFGEdgeAnalyzationVisitor(methodName, methodNode);
            methodNode.accept(cfgEdgeAnalysationVisitor);
            return cfgEdgeAnalysationVisitor.getEdges();
        }

        private void addDummyStartNode() {
            final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();
            for (int i = 0; i < parameterTypes.length; i++) {
                final Optional<LocalVariable> parameterVariable = localVariableTable.getEntry(i + 1);
                if (parameterVariable.isPresent()) {
                    final LocalVariable localVariable = parameterVariable.get();
                    final ProgramVariable variable =
                            ProgramVariable.create(
                                    localVariable.getName(), localVariable.getDescriptor(), Integer.MIN_VALUE);
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

