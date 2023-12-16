package graphs.cfg.visitors.methodVisitors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.MethodData;
import data.ProgramVariable;
import data.ProjectData;
import graphs.cfg.CFG;
import graphs.cfg.LocalVariable;
import graphs.cfg.nodes.CFGCallNode;
import graphs.cfg.nodes.CFGEntryNode;
import graphs.cfg.nodes.CFGExitNode;
import graphs.cfg.nodes.CFGNode;
import graphs.cfg.visitors.classVisitors.CFGClassVisitor;
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
public class CFGMethodVisitor extends JDFCMethodVisitor {
    private final Multimap<Integer, Integer> edges;
    private final NavigableMap<Integer, CFGNode> nodes;
    private final MethodData mData;
    private final CFGAnalyzerAdapter aa;
    private final int argCount;
    private final boolean isStatic;
    private ASMHelper asmHelper;

    public CFGMethodVisitor(final CFGClassVisitor pClassVisitor,
                            final MethodVisitor pMethodVisitor,
                            final MethodNode pMethodNode,
                            final String pInternalMethodName,
                            final CFGAnalyzerAdapter aa,
                            int argCount) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
        this.edges = ArrayListMultimap.create();
        this.nodes = Maps.newTreeMap();

        this.mData = pClassVisitor.classData.getMethodByInternalName(internalMethodName);
        this.aa = aa;
        this.argCount = argCount;
        this.asmHelper = new ASMHelper();
        this.isStatic = this.asmHelper.isStatic(mData.getAccess());
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
//        logger.debug("visitFrame");
        super.visitFrame(type, numLocal, local, numStack, stack);
        aa.visitFrame(type, numLocal, local, numStack, stack);
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                getFrameOpcode(type));
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        aa.visitInsn(opcode);
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
//        String debug = String.format("visitIntInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitIntInsn(opcode, operand);
        aa.visitIntInsn(opcode, operand);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
//        String debug = String.format("visitVarInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        updateCurrentNode();
        checkForF_NEW();
        super.visitVarInsn(opcode, var);
        createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
        aa.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        aa.visitTypeInsn(opcode, type);
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        createCFGNodeForFieldInsnNode(opcode, owner, name, descriptor, currentInstructionIndex, currentLineNumber);
        aa.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // opcode, name of called class, name of called method, desc of called method
//        String debug = String.format("visitMethodInsn %s", JDFCUtils.getOpcode(opcode));
//        logger.debug(debug);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        aa.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        ASMHelper asmHelper = new ASMHelper();
        String shortCalledMethodName = asmHelper.computeInternalMethodName(
                name,
                descriptor,
                null,
                null);
        MethodData cmData = classVisitor.classData.getMethodByShortInternalName(shortCalledMethodName);
        if (owner.equals(classVisitor.classNode.name) && cmData != null) {
            if (cmData.getName().contains("addObservedPoint")) {
                System.out.println();
            }
            Map<Integer, ProgramVariable> paramPositionMap = this.createIndexUseMap(aa.getPopList(), cmData);
            if (!paramPositionMap.isEmpty()) {
                paramPositionMap = this.cleanupPVarMap(paramPositionMap, cmData);
            }

            CFGCallNode node = new CFGCallNode(
                    currentInstructionIndex,
                    opcode,
                    classVisitor.classNode.name,
                    internalMethodName,
                    this.mData.getAccess(),
                    currentLineNumber,
                    owner,
                    cmData.buildInternalMethodName(),
                    isInterface,
                    paramPositionMap);
            nodes.put(currentInstructionIndex, node);
        } else {
            final CFGNode node = new CFGNode(
                    classVisitor.classNode.name,
                    internalMethodName,
                    this.mData.getAccess(),
                    currentLineNumber,
                    currentInstructionIndex,
                    opcode);
            nodes.put(currentInstructionIndex, node);
        }
    }

    /**
     * This method exists, because on some occasions more than just the method parameters are popped from the operand
     * stack, which leads to issues when we want to match parameters on call and entry nodes.
     * Therefore we want to delete all ProgramVariables that are not part of the called methods scope.
     * We use the first local variable from the called methods variable table and compare its type to the type of
     * the variables in the pop list.
     * As soon as these types match we assume that the matching element is the first parameter to match followed by all
     * other parameters, which require matching.
     * This is not sure to work at all times.
     * @param pVarMap
     * @param called
     * @return
     */
    private Map<Integer, ProgramVariable> cleanupPVarMap(Map<Integer, ProgramVariable> pVarMap, MethodData called) {
        Map<Integer, ProgramVariable> newMap = new HashMap<>();
        Optional<LocalVariable> firstOptional = called.getLocalVariableTable().values().stream().findFirst();
        if (firstOptional.isPresent()) {
            LocalVariable first = firstOptional.get();
            String descriptor = first.getDescriptor();
            int key = -1;
            boolean foundFirst = false;
            pVarMap.values().removeIf(Objects::isNull);
            for (Map.Entry<Integer, ProgramVariable> entry : pVarMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (!foundFirst && entry.getValue().getDescriptor().equals(descriptor)) {
                        foundFirst = true;
                        key = entry.getKey();
                    }
                }
            }

            int removed = 0;
            for (int i = 0; i < key; i++) {
                if (pVarMap.get(i) != null) {
                    pVarMap.remove(i);
                    removed++;
                }
            }

            for (Map.Entry<Integer, ProgramVariable> entry : pVarMap.entrySet()) {
                newMap.put(entry.getKey() - removed, entry.getValue());
            }
        }
        return newMap;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
//        logger.debug("visitInvokeDynamicInsn");
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        aa.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                INVOKEDYNAMIC);
        JDFCUtils.logThis(String.format("%s %s %s %s %s", "invokedynamic", name, descriptor, bootstrapMethodHandle, Arrays.toString(bootstrapMethodArguments)), "dynamic_insn");
        nodes.put(currentInstructionIndex, node);
    }



    @Override
    public void visitJumpInsn(int opcode, Label label) {
//        String debug = String.format("visitJumpInsn %s %s", JDFCUtils.getOpcode(opcode), label);
//        logger.debug(debug);
        super.visitJumpInsn(opcode, label);
        String debug = JDFCUtils.getOpcode(opcode);
        aa.visitJumpInsn(opcode, label);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLdcInsn(Object value) {
//        logger.debug("visitLdcInsn");
        super.visitLdcInsn(value);
        aa.visitLdcInsn(value);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                LDC);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
//        logger.debug("visitIincInsn");
        updateCurrentNode();
        checkForF_NEW();
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
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                TABLESWITCH);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
//        logger.debug("visitLookupSwitchInsn");
        super.visitLookupSwitchInsn(dflt, keys, labels);
        aa.visitLookupSwitchInsn(dflt, keys, labels);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                LOOKUPSWITCH);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
//        logger.debug("visitMultiANewArrayInsn");
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        aa.visitMultiANewArrayInsn(descriptor, numDimensions);
//        checkForF_NEW();
        final CFGNode node = new CFGNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                currentInstructionIndex,
                MULTIANEWARRAY);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitEnd() {
//        logger.debug("visitEnd");
        super.visitEnd();

        edges.putAll(createEdges());
        this.addEntryAndExitNode();

        this.setPredecessorSuccessorRelation();
        CFG cfg = new CFG(classVisitor.classNode.name, internalMethodName, nodes, edges);

        if (true) {
            JDFCUtils.logThis(JDFCUtils.prettyPrintMap(nodes), String.format("cfg_%s::%s", classVisitor.classData.getClassMetaData().getName(), methodNode.name));
            JDFCUtils.logThis(JDFCUtils.prettyPrintMultimap(edges), String.format("cfg_%s::%s", classVisitor.classData.getClassMetaData().getName(), methodNode.name));
        }

        if (!internalMethodName.contains("<clinit>")) {
            MethodData mData = classVisitor.classData.getMethodByInternalName(internalMethodName);
            mData.setCfg(cfg);
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
            final CFGNode node = new CFGNode(
                    classVisitor.classNode.name,
                    internalMethodName,
                    this.mData.getAccess(),
                    currentLineNumber,
                    currentInstructionIndex,
                    F_NEW);
            nodes.put(currentInstructionIndex, node);
            updateCurrentNode();
        }
    }

    private ProgramVariable getProgramVariableFromLocalVar(final int localVarIdx,
                                                           final int opcode,
                                                           final int insnIdx,
                                                           final int lineNumber) {
//        logger.debug(String.format("getProgramVariableFromLocalVar(%d, %d, %d, %d)", localVarIdx, opcode, insnIdx, lineNumber));
        final String varName = getLocalVarName(localVarIdx);
        final String varType = getLocalVarType(localVarIdx);
        final boolean isDefinition = isDefinition(opcode);
        UUID id = UUID.randomUUID();
        ProgramVariable var = new ProgramVariable(
                id,
                localVarIdx,
                mData.getClassName(),
                mData.buildInternalMethodName(),
                varName,
                varType,
                insnIdx,
                lineNumber,
                isDefinition,
                false,
                false
        );
        ProjectData.getInstance().getProgramVariableMap().put(id, var);
        mData.getPVarIds().add(id);
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
        // TODO: Check the difference between descriptor and signature
        final LocalVariable localVariable = mData.getLocalVariableTable().get(pVarNumber);
        if (localVariable != null) {
            return localVariable.getDescriptor();
        } else {
            return "UNKNOWN";
        }
    }

    private void createCFGNodeForFieldInsnNode(final int opcode,
                                               final String owner,
                                               final String name,
                                               final String descriptor,
                                               final int insnIdx,
                                               final int lineNumber) {
        CFGNode node;
        ProgramVariable programVariable;
        switch(opcode) {
            case PUTFIELD:
            case PUTSTATIC:
                programVariable = new ProgramVariable(
                        UUID.randomUUID(),
                        Integer.MIN_VALUE,
                        owner,
                        internalMethodName,
                        name,
                        descriptor,
                        insnIdx,
                        lineNumber,
                        true,
                        false,
                        true
                );
                ProjectData.getInstance().getProgramVariableMap().put(programVariable.getId(), programVariable);
                mData.getPVarIds().add(programVariable.getId());
//                classVisitor.classData.getFieldDefinitions().computeIfAbsent(mData.getId(), k -> new HashMap<>());
//                classVisitor.classData.getFieldDefinitions().get(mData.getId()).put(programVariable.getId(), programVariable);
                node = new CFGNode(
                        classVisitor.classNode.name,
                        internalMethodName,
                        this.mData.getAccess(),
                        currentLineNumber,
                        Sets.newHashSet(programVariable),
                        Sets.newLinkedHashSet(),
                        insnIdx,
                        opcode);
                break;
            case GETFIELD:
            case GETSTATIC:
                programVariable = new ProgramVariable(
                        UUID.randomUUID(),
                        Integer.MIN_VALUE,
                        owner,
                        "test",
                        name,
                        descriptor,
                        insnIdx,
                        lineNumber,
                        false,
                        false,
                        true
                );
                ProjectData.getInstance().getProgramVariableMap().put(programVariable.getId(), programVariable);
                mData.getPVarIds().add(programVariable.getId());
                node = new CFGNode(
                        classVisitor.classNode.name,
                        internalMethodName,
                        this.mData.getAccess(),
                        currentLineNumber,
                        Sets.newLinkedHashSet(),
                        Sets.newHashSet(programVariable),
                        insnIdx,
                        opcode);
                break;
            default:
                node = new CFGNode(
                        classVisitor.classNode.name,
                        internalMethodName,
                        this.mData.getAccess(),
                        currentLineNumber,
                        insnIdx,
                        opcode);
                break;
        }
        nodes.put(insnIdx, node);
    }

    private void createCFGNodeForVarInsnNode(final int opcode, final int localVarIdx, final int insnIdx, final int lineNumber) {
//        logger.debug("createCFGNodeForVarInsnNode");
        final CFGNode node;
        final ProgramVariable programVariable;
        switch (opcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                programVariable = getProgramVariableFromLocalVar(localVarIdx, opcode, insnIdx, lineNumber);
                node = new CFGNode(
                        classVisitor.classNode.name,
                        internalMethodName,
                        this.mData.getAccess(),
                        currentLineNumber,
                        Sets.newHashSet(programVariable),
                        Sets.newLinkedHashSet(),
                        insnIdx,
                        opcode);
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                programVariable = getProgramVariableFromLocalVar(localVarIdx, opcode, insnIdx, lineNumber);
                node = new CFGNode(
                        classVisitor.classNode.name,
                        internalMethodName,
                        this.mData.getAccess(),
                        currentLineNumber,
                        Sets.newLinkedHashSet(),
                        Sets.newHashSet(programVariable),
                        insnIdx,
                        opcode);
                break;
            default:
                node = new CFGNode(
                        classVisitor.classNode.name,
                        internalMethodName,
                        this.mData.getAccess(),
                        currentLineNumber,
                        insnIdx,
                        opcode);
                break;
        }
        nodes.put(insnIdx, node);
    }

    private void createCFGNodeForIincInsnNode(final int varNumber, final int pIndex, final int pLineNumber) {
//        logger.debug("createCFGNodeForIincInsnNode");
        final ProgramVariable programVariable = getProgramVariableFromLocalVar(varNumber, ISTORE, pIndex, pLineNumber);
        final CFGNode node =
                new CFGNode(
                        classVisitor.classNode.name,
                        internalMethodName,
                        this.mData.getAccess(),
                        currentLineNumber,
                        Sets.newHashSet(programVariable),
                        Sets.newHashSet(programVariable),
                        pIndex,
                        IINC);
        nodes.put(pIndex, node);
    }

    private Map<Integer, ProgramVariable> createIndexUseMap(List<Object> popList, MethodData called) {
        List<Object> local = new ArrayList<>(popList);
        Map<Integer, ProgramVariable> result = new HashMap<>();
        // Reverse list is necessary, because arguments are popped from the stack in reverse order
        Collections.reverse(local);
        // Add "this" if called procedure is not static
        int index = 0;
        if (!asmHelper.isStatic(called.getAccess()) && !isStatic) {
            ProgramVariable p = createPVarThis(currentInstructionIndex);
            if (p != null) {
                result.put(index, p);
                index++;
            }
        }

        // Match params to their position in method call
        List<String> paramTypes = asmHelper.extractParameterTypes(called.toString());
        while (!paramTypes.isEmpty()) {
            String type = paramTypes.remove(0);
            if (!local.isEmpty()) {
                Object a = local.remove(0);
                if (Objects.equals(type, "D") || Objects.equals(type, "J")) {
                    if (!local.isEmpty()) {
                        // could be empty when exception occurs
                        Object b = local.remove(0);
                        if (!(a instanceof ProgramVariable || b instanceof ProgramVariable)) {
                            result.put(index, null);
                        } else if (a instanceof ProgramVariable) {
                            result.put(index, (ProgramVariable) a);
                        } else {
                            result.put(index, (ProgramVariable) b);
                        }
                    }
                } else {
                    if (a instanceof ProgramVariable) {
                        result.put(index, (ProgramVariable) a);
                    } else {
                        result.put(index, null);
                    }
                }
                index++;
            }
        }
        return result;
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
            first.getSucc().add(second);
            second.getPred().add(first);
        }
    }

    private void addEntryAndExitNode() {
//        logger.debug("addEntryAndExitNode");
        Set<ProgramVariable> definitions = createDefinitionsFromLocalVars();
        Map<Integer, ProgramVariable> pVarMap = new HashMap<>();
        int idx = 0;
        for(ProgramVariable def : definitions) {
            ProjectData.getInstance().getProgramVariableMap().put(def.getId(), def);
            mData.getPVarIds().add(def.getId());
            pVarMap.put(idx, def);
            idx++;
        }

        // Copy nodes
        NavigableMap<Integer, CFGNode> tempNodes = Maps.newTreeMap();
        tempNodes.putAll(this.nodes);

        // Put entry node
        final CFGNode entryNode = new CFGEntryNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                definitions,
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet(),
                pVarMap);
        entryNode.setIndex(0);
        nodes.put(0, entryNode);

        // Put all other nodes
        for(Map.Entry<Integer, CFGNode> entry : tempNodes.entrySet()) {
            int index = entry.getKey() + 1;
            CFGNode cfgNode = entry.getValue();
            cfgNode.setIndex(index);
            nodes.put(index, cfgNode);
        }

        // Put exit node
        final CFGNode exitNode = new CFGExitNode(
                classVisitor.classNode.name,
                internalMethodName,
                this.mData.getAccess(),
                currentLineNumber,
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet(),
                Sets.newHashSet(),
                Sets.newLinkedHashSet(),
                pVarMap);
        int index = nodes.size();
        exitNode.setIndex(index);
        nodes.put(index, exitNode);

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

    private ProgramVariable createPVarThis(int insnIndex) {
        ProgramVariable var = null;
        for(ProgramVariable p : mData.getPVarsFromStore().values()) {
            if(var == null  && p.getName().equals("this")) {
                var = p;
            }

            if(p.getName().equals("this")
                    && p.getInstructionIndex() < insnIndex
                    && var.getInstructionIndex() < p.getInstructionIndex()) {
                var = p;
            }

        }
        return var;
    }

    /**
     * Creates a program variable from every variable in the local variable table.<br>
     * This includes "this" (the current object reference) and all variables that are defined locally
     * throughout method execution.
     *
     * @return A set of program variables containing all variables from the local variable table.
     */
    private Set<ProgramVariable> createDefinitionsFromLocalVars() {
        final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();
        int threshold = argCount;
        for (LocalVariable localVariable : mData.getLocalVariableTable().values()) {
            if(localVariable.getDescriptor().equals("J") || localVariable.getDescriptor().equals("D")) {
                threshold++;
            }
        }
        for (Map.Entry<Integer, LocalVariable> entry : mData.getLocalVariableTable().entrySet()) {
            if ((isStatic && entry.getKey() < threshold) || (!isStatic && entry.getKey() <= threshold)) {
                final ProgramVariable variable =
                        new ProgramVariable(
                                UUID.randomUUID(),
                                entry.getValue().getIndex(),
                                mData.getClassName(),
                                mData.buildInternalMethodName(),
                                entry.getValue().getName(),
                                entry.getValue().getDescriptor(),
                                Integer.MIN_VALUE,
                                Integer.MIN_VALUE,
                                true,
                                false,
                                false);
                parameters.add(variable);
            }
        }
       return parameters;
    }
}
