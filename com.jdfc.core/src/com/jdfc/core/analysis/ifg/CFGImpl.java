package com.jdfc.core.analysis.ifg;


import com.google.common.base.Preconditions;
import com.jdfc.commons.data.Pair;
import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

/**
 * A implementation of a {@link CFG}.
 */
public class CFGImpl implements CFG {

    private final String methodName;
    private final NavigableMap<Integer, CFGNode> nodes;
    private final LocalVariableTable localVariableTable;
    private final boolean isImpure;

    CFGImpl(
            final String pMethodName,
            final NavigableMap<Integer, CFGNode> pNodes,
            final LocalVariableTable pLocalVariableTable,
            final boolean pIsImpure) {
        Preconditions.checkNotNull(pMethodName);
        Preconditions.checkNotNull(pNodes);
        methodName = pMethodName;
        nodes = pNodes;
        localVariableTable = pLocalVariableTable;
        isImpure = pIsImpure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NavigableMap<Integer, CFGNode> getNodes() {
        return nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalVariableTable getLocalVariableTable() {
        return localVariableTable;
    }

    @Override
    public boolean isImpure() {
        return isImpure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void calculateReachingDefinitions() {

        // TODO: update reaching definitions
        LinkedList<CFGNode> workList = new LinkedList<>();
        for (Map.Entry<Integer, CFGNode> node : nodes.entrySet()) {
            node.getValue().resetReachOut();
            workList.add(node.getValue());
        }

        while (!workList.isEmpty()) {
            CFGNode node = workList.poll();
            Set<ProgramVariable> oldValue = node.getReachOut();
            node.update();
            if (!node.getReachOut().equals(oldValue)) {
                node.getSuccessors().forEach(workList::addLast);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("CFGImpl for method %s (containing %d nodes)", methodName, nodes.size());
    }

    public static void addLocalVarCoveredEntry(final String pClassName,
                                               final String pMethodName,
                                               final String pMethodDesc,
                                               final int pVarIndex,
                                               final int pInsnIndex,
                                               final int pLineNumber) {
        String methodNameDesc = pMethodName.concat(": " + pMethodDesc);
        ClassExecutionData classExecutionData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        ProgramVariable programVariable = prepareNewLocalVarEntry(classExecutionData, methodNameDesc, pVarIndex, pInsnIndex, pLineNumber);
        addCoveredEntry(pClassName, methodNameDesc, classExecutionData, programVariable);
    }

    public static void addInstanceVarCoveredEntry(final String pClassName,
                                                  final String pOwner,
                                                  final String pMethodName,
                                                  final String pMethodDesc,
                                                  final String pVarName,
                                                  final String pVarDesc,
                                                  final int pIndex,
                                                  final int pLineNumber) {
        String methodNameDesc = pMethodName.concat(": " + pMethodDesc);
        ClassExecutionData classExecutionData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        ProgramVariable programVariable = prepareNewInstanceVarEntry(classExecutionData, pOwner, pVarName, pVarDesc, pIndex, pLineNumber);
        addCoveredEntry(pClassName, methodNameDesc, classExecutionData, programVariable);
    }

    static ProgramVariable prepareNewLocalVarEntry(final ClassExecutionData pData,
                                                   final String pMethodName,
                                                   final int pVarIndex,
                                                   final int pInsnIndex,
                                                   final int pLineNumber) {
        CFG cfg = pData.getMethodCFGs().get(pMethodName);
        LocalVariableTable table = cfg.getLocalVariableTable();
        LocalVariable variable = findLocalVariable(table, pVarIndex);
        if (variable != null) {
            return ProgramVariable.create(null, variable.getName(), variable.getDescriptor(), pInsnIndex, pLineNumber);
        }
        return null;
    }

    static ProgramVariable prepareNewInstanceVarEntry(final ClassExecutionData pClassExecutionData,
                                                      final String pOwner,
                                                      final String pVarName,
                                                      final String pVarDesc,
                                                      final int pInstructionIndex,
                                                      final int pLineNumber) {
        if(pClassExecutionData != null) {
            Set<InstanceVariable> set = pClassExecutionData.getInstanceVariables();
            InstanceVariable variable = findInstanceVariable(set, pOwner, pVarName, pVarDesc);
            if (variable != null) {
                return ProgramVariable.create(variable.getOwner(), variable.getName(), variable.getDescriptor(), pInstructionIndex, pLineNumber);
            }
        }
        return null;
    }

    private static void addCoveredEntry(String pClassName, String methodNameDesc, ClassExecutionData classExecutionData, ProgramVariable programVariable) {
        if (programVariable != null) {
            Map<String, Set<ProgramVariable>> coveredList = classExecutionData.getDefUseCovered();
            coveredList.get(methodNameDesc).add(programVariable);
            try {
                dumpToFile(pClassName, classExecutionData);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        }
    }

    static LocalVariable findLocalVariable(LocalVariableTable table, int index) {
        Optional<LocalVariable> o = table.getEntry(index);
        return o.orElse(null);
    }

    static InstanceVariable findInstanceVariable(Set<InstanceVariable> pSet, String pOwner, String pVarName, String pVarDesc) {
        for (InstanceVariable variable : pSet) {
            if (variable.getOwner().equals(pOwner)
                    && variable.getName().equals(pVarName)
                    && variable.getDescriptor().equals(pVarDesc)) {
                return variable;
            }
        }
        return null;
    }

    // TODO: Either create own class to handle xml or figure out how to do it jacoco like
    static void dumpToFile(final String pClassName,
                           final ClassExecutionData pClassExecutionData) throws ParserConfigurationException, TransformerException {
        String outPath = String.format("%s/target/jdfc", System.getProperty("user.dir"));
        File jdfcDir = new File(outPath);
        if (!jdfcDir.exists()) {
            jdfcDir.mkdirs();
        }

        String classXMLPath = String.format("%s/%s.xml", outPath, pClassName);
        TreeMap<String, List<DefUsePair>> defUsePairs = pClassExecutionData.getDefUsePairs();
        Map<String, Set<ProgramVariable>> defUseCovered = pClassExecutionData.getDefUseCovered();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element rootTag = doc.createElement("root");
        doc.appendChild(rootTag);

        Element instanceVariablesTag = doc.createElement("instanceVariables");
        rootTag.appendChild(instanceVariablesTag);
        for (InstanceVariable instanceVariable : pClassExecutionData.getInstanceVariables()) {
            Element instanceVarTag = doc.createElement("instanceVariable");
            instanceVarTag.setAttribute("owner", instanceVariable.getOwner());
            instanceVarTag.setAttribute("access", String.valueOf(instanceVariable.getAccess()));
            instanceVarTag.setAttribute("name", instanceVariable.getName());
            instanceVarTag.setAttribute("descriptor", instanceVariable.getDescriptor());
            instanceVarTag.setAttribute("signature", instanceVariable.getSignature());
            instanceVarTag.setAttribute("lineNumber", String.valueOf(instanceVariable.getLineNumber()));
            instanceVariablesTag.appendChild(instanceVarTag);
        }

        Element parameterMatching = doc.createElement("parameterMatching");
        rootTag.appendChild(parameterMatching);
        for(Pair<ProgramVariable, ProgramVariable> matching : pClassExecutionData.getParameterMatching()) {
            Element match = doc.createElement("match");
            parameterMatching.appendChild(match);
            Element var1 = doc.createElement("programVariable");
            match.appendChild(var1);
            Element var2 = doc.createElement("programVariable");
            match.appendChild(var2);
            var1.setAttribute("owner", matching.fst.getOwner());
            var1.setAttribute("name", matching.fst.getName());
            var1.setAttribute("type", matching.fst.getType());
            var1.setAttribute("instructionIndex",
                    Integer.toString(matching.fst.getInstructionIndex()));
            var1.setAttribute("lineNumber",
                    Integer.toString(matching.fst.getLineNumber()));
            var2.setAttribute("owner", matching.snd.getOwner());
            var2.setAttribute("name", matching.snd.getName());
            var2.setAttribute("type", matching.snd.getType());
            var2.setAttribute("instructionIndex",
                    Integer.toString(matching.snd.getInstructionIndex()));
            var2.setAttribute("lineNumber",
                    Integer.toString(matching.snd.getLineNumber()));
        }

        for (Map.Entry<String, List<DefUsePair>> methodEntry : defUsePairs.entrySet()) {
            if (methodEntry.getValue().size() == 0) {
                continue;
            }
            Element methodTag = doc.createElement("method");
            methodTag.setAttribute("name", methodEntry.getKey());
            rootTag.appendChild(methodTag);

            Element defUsePairListTag = doc.createElement("defUsePairs");
            methodTag.appendChild(defUsePairListTag);

            for (DefUsePair duPair : methodEntry.getValue()) {
                Element defUsePairTag = doc.createElement("defUsePair");
                defUsePairListTag.appendChild(defUsePairTag);
                defUsePairTag.setAttribute("dOwner", duPair.getDefinition().getOwner());
                defUsePairTag.setAttribute("dName", duPair.getDefinition().getName());
                defUsePairTag.setAttribute("dType", duPair.getDefinition().getType());
                defUsePairTag.setAttribute("dIndex", Integer.toString(duPair.getDefinition().getInstructionIndex()));
                defUsePairTag.setAttribute("dLineNumber", Integer.toString(duPair.getDefinition().getLineNumber()));

                defUsePairTag.setAttribute("uOwner", duPair.getUsage().getOwner());
                defUsePairTag.setAttribute("uName", duPair.getUsage().getName());
                defUsePairTag.setAttribute("uType", duPair.getUsage().getType());
                defUsePairTag.setAttribute("uIndex", Integer.toString(duPair.getUsage().getInstructionIndex()));
                defUsePairTag.setAttribute("uLineNumber", Integer.toString(duPair.getUsage().getLineNumber()));
            }

            Element defUseCoveredTag = doc.createElement("defUseCovered");
            methodTag.appendChild(defUseCoveredTag);

            if (defUseCovered.get(methodEntry.getKey()) != null) {
                for (ProgramVariable programVariable : defUseCovered.get(methodEntry.getKey())) {
                    Element programVariableTag = doc.createElement("programVariable");
                    programVariableTag.setAttribute("owner", programVariable.getOwner());
                    programVariableTag.setAttribute("name", programVariable.getName());
                    programVariableTag.setAttribute("type", programVariable.getType());
                    programVariableTag.setAttribute("instructionIndex",
                            Integer.toString(programVariable.getInstructionIndex()));
                    programVariableTag.setAttribute("lineNumber",
                            Integer.toString(programVariable.getLineNumber()));
                    defUseCoveredTag.appendChild(programVariableTag);
                }
            }
        }

        Element methodRangeMap = doc.createElement("methodRangeMap");
        rootTag.appendChild(methodRangeMap);

        for (Map.Entry<String, Pair<Integer, Integer>> methodEntry : pClassExecutionData.getMethodRangeMap().entrySet()) {
            Element methodRange = doc.createElement("methodRange");
            methodRangeMap.appendChild(methodRange);
            methodRange.setAttribute("methodName", methodEntry.getKey());
            methodRange.setAttribute("fst", String.valueOf(methodEntry.getValue().fst));
            methodRange.setAttribute("snd", String.valueOf(methodEntry.getValue().snd));
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource domSource = new DOMSource(doc);
        File file = new File(classXMLPath);
        file.getParentFile().mkdirs();
        StreamResult streamResult = new StreamResult(file);
        transformer.transform(domSource, streamResult);
    }
}
