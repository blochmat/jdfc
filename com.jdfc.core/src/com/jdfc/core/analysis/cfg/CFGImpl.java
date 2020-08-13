package com.jdfc.core.analysis.cfg;


import com.google.common.base.Preconditions;
import com.jdfc.core.analysis.CFGStorage;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;

/** A implementation of a {@link CFG}. */
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

    /** {@inheritDoc} */
    @Override
    public Map<Integer, CFGNode> getNodes() {
        return nodes;
    }

    /** {@inheritDoc} */
    @Override
    public LocalVariableTable getLocalVariableTable() {
        return localVariableTable;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("CFGImpl for method %s (containing %d nodes)", methodName, nodes.size());
    }

    public static void addCoveredEntry(
            String methodName, String methodDesc, int varIndex, int instructionIndex) {
        String methodNameDesc = methodName.concat(": " + methodDesc);
        ProgramVariable programVariable = prepareNewEntry(methodNameDesc, varIndex, instructionIndex);
        Map<String, Set<ProgramVariable>> coveredList = CFGStorage.INSTANCE.getDefUseCovered();
        coveredList.get(methodNameDesc).add(programVariable);
        try {
            dumpToFile();
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    static ProgramVariable prepareNewEntry(String methodName, int varIndex, int instructionIndex) {
        CFG cfg = CFGStorage.INSTANCE.getMethodCFGs().get(methodName);
        LocalVariableTable table = cfg.getLocalVariableTable();
        LocalVariable variable = findLocalVariable(table, varIndex);
        return ProgramVariable.create(variable.getName(), variable.getDescriptor(), instructionIndex);
    }

    static LocalVariable findLocalVariable(LocalVariableTable table, int index) {
        Optional<LocalVariable> o = table.getEntry(index);
        return o.orElse(null);
    }

    static void dumpToFile() throws ParserConfigurationException, TransformerException {
        String outPath = String.format("%s/target/output.xml", System.getProperty("user.dir"));

        TreeMap<String, List<DefUsePair>> defUsePairs = CFGStorage.INSTANCE.getDefUsePairs();
        Map<String, Set<ProgramVariable>> defUseCovered = CFGStorage.INSTANCE.getDefUseCovered();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element root = doc.createElement("root");
        doc.appendChild(root);

        for(Map.Entry<String, List<DefUsePair>> methodEntry: defUsePairs.entrySet()){
            if (methodEntry.getValue().size()==0){
                continue;
            }
            Element method = doc.createElement("method");
            root.appendChild(method);
            Attr methodName = doc.createAttribute("name");
            methodName.setValue(methodEntry.getKey());
            method.setAttributeNode(methodName);
            method.setAttribute("total", Integer.toString((methodEntry.getValue().size())));

            for(DefUsePair duPair : methodEntry.getValue()){
                ProgramVariable variableDef = duPair.getDefinition();
                ProgramVariable variableUse = duPair.getUsage();

                Element pair = doc.createElement("pair");
                method.appendChild(pair);
                boolean pairCovered = defUseCovered.get(methodEntry.getKey()).contains(duPair.getDefinition())
                        && defUseCovered.get(methodEntry.getKey()).contains(duPair.getUsage());
                pair.setAttribute("covered", Boolean.toString(pairCovered));
                pair.setAttribute("name", variableDef.getName());
                pair.setAttribute("type", variableDef.getType());
                pair.setAttribute("defIndex", Integer.toString(variableDef.getInstructionIndex()));
                pair.setAttribute("useIndex", Integer.toString(variableUse.getInstructionIndex()));
            }
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(new File(outPath));
        transformer.transform(domSource, streamResult);
    }
}
