package com.jdfc.core.analysis.cfg;


import com.google.common.base.Preconditions;
import com.jdfc.core.analysis.CoverageDataStore;
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
    private final Map<Integer, CFGNode> nodes;
    private final LocalVariableTable localVariableTable;

    CFGImpl(
            final String pMethodName,
            final Map<Integer, CFGNode> pNodes,
            final LocalVariableTable pLocalVariableTable) {
        Preconditions.checkNotNull(pMethodName);
        Preconditions.checkNotNull(pNodes);
        methodName = pMethodName;
        nodes = pNodes;
        localVariableTable = pLocalVariableTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, CFGNode> getNodes() {
        return nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalVariableTable getLocalVariableTable() {
        return localVariableTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void calculateReachingDefinitions() {
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
        ClassExecutionData classNodeData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        ProgramVariable programVariable = prepareNewLocalVarEntry(classNodeData, methodNameDesc, pVarIndex, pInsnIndex, pLineNumber);
        if(programVariable != null) {
            Map<String, Set<ProgramVariable>> coveredList = classNodeData.getDefUseCovered();
            coveredList.get(methodNameDesc).add(programVariable);
            try {
                dumpToFile(pClassName);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        }
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
        ProgramVariable programVariable = prepareNewInstanceVarEntry(pClassName, pOwner, pVarName, pVarDesc, pIndex, pLineNumber);
        if (programVariable != null) {
            ClassExecutionData classNodeData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
            Map<String, Set<ProgramVariable>> coveredList = classNodeData.getDefUseCovered();
            coveredList.get(methodNameDesc).add(programVariable);
            try {
                dumpToFile(pClassName);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        }
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

    static ProgramVariable prepareNewInstanceVarEntry(final String pClassName,
                                                   final String pOwner,
                                                   final String pVarName,
                                                   final String pVarDesc,
                                                   final int pInstructionIndex,
                                                   final int pLineNumber) {
        ClassExecutionData classNodeData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        Set<InstanceVariable> set = classNodeData.getInstanceVariables();
        InstanceVariable variable = findInstanceVariable(set, pOwner, pVarName, pVarDesc);
        if(variable != null) {
            return ProgramVariable.create(variable.getOwner(), variable.getName(), variable.getDescriptor(), pInstructionIndex, pLineNumber);
        }
        return null;
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
    static void dumpToFile(String pClassName) throws ParserConfigurationException, TransformerException {
        String outPath = String.format("%s/target/jdfc", System.getProperty("user.dir"));
        File jdfcDir = new File(outPath);
        if (!jdfcDir.exists()) {
            jdfcDir.mkdirs();
        }

        String classXMLPath = String.format("%s/%s.xml", outPath, pClassName);
        ClassExecutionData classData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        TreeMap<String, List<DefUsePair>> defUsePairs = classData.getDefUsePairs();
        Map<String, Set<ProgramVariable>> defUseCovered = classData.getDefUseCovered();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element rootTag = doc.createElement("root");
        doc.appendChild(rootTag);

        Element instanceVariablesTag = doc.createElement("instanceVariables");
        rootTag.appendChild(instanceVariablesTag);
        for (InstanceVariable instanceVariable : classData.getInstanceVariables()) {
            Element instanceVarTag = doc.createElement("instanceVariable");
            instanceVarTag.setAttribute("owner", instanceVariable.getOwner());
            instanceVarTag.setAttribute("access", String.valueOf(instanceVariable.getAccess()));
            instanceVarTag.setAttribute("name", instanceVariable.getName());
            instanceVarTag.setAttribute("descriptor", instanceVariable.getDescriptor());
            instanceVarTag.setAttribute("signature", instanceVariable.getSignature());
            instanceVarTag.setAttribute("lineNumber", String.valueOf(instanceVariable.getLineNumber()));
            instanceVariablesTag.appendChild(instanceVarTag);
        }

        for (Map.Entry<String, List<DefUsePair>> methodEntry : defUsePairs.entrySet()) {
            if (methodEntry.getValue().size() == 0) {
                continue;
            }
            Element methodTag = doc.createElement("method");
            methodTag.setAttribute("name", methodEntry.getKey());
            rootTag.appendChild(methodTag);

            Element defUsePairsTag = doc.createElement("defUsePairs");
            methodTag.appendChild(defUsePairsTag);

            for (DefUsePair duPair : methodEntry.getValue()) {
                Element defUsePairTag = doc.createElement("defUsePair");
                defUsePairTag.setAttribute("owner", duPair.getDefinition().getOwner());
                defUsePairTag.setAttribute("name", duPair.getDefinition().getName());
                defUsePairTag.setAttribute("type", duPair.getDefinition().getType());
                defUsePairTag.setAttribute("definitionIndex", Integer.toString(duPair.getDefinition().getInstructionIndex()));
                defUsePairTag.setAttribute("definitionLineNumber", Integer.toString(duPair.getDefinition().getLineNumber()));

                defUsePairTag.setAttribute("usageIndex", Integer.toString(duPair.getUsage().getInstructionIndex()));
                defUsePairTag.setAttribute("usageLineNumber", Integer.toString(duPair.getUsage().getLineNumber()));
                defUsePairsTag.appendChild(defUsePairTag);
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
