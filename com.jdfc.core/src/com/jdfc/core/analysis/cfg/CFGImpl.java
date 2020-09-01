package com.jdfc.core.analysis.cfg;


import com.google.common.base.Preconditions;
import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.internal.data.ClassNodeData;
import org.w3c.dom.*;

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
            String className, String methodName, String methodDesc, int varIndex, int instructionIndex) {
        String methodNameDesc = methodName.concat(": " + methodDesc);
        ProgramVariable programVariable = prepareNewEntry(className, methodNameDesc, varIndex, instructionIndex);

        // TODO
        ClassNodeData classNodeData = CoverageDataStore.INSTANCE.findClassDataNode(className);
        Map<String, Set<ProgramVariable>> coveredList = classNodeData.getDefUseCovered();
        coveredList.get(methodNameDesc).add(programVariable);
        try {
            dumpToFile(className);
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    static ProgramVariable prepareNewEntry(String className, String methodName, int varIndex, int instructionIndex) {

        // TODO
        ClassNodeData classNodeData = CoverageDataStore.INSTANCE.findClassDataNode(className);
        assert classNodeData != null;
        CFG cfg = classNodeData.getMethodCFGs().get(methodName);


        LocalVariableTable table = cfg.getLocalVariableTable();
        LocalVariable variable = findLocalVariable(table, varIndex);
        return ProgramVariable.create(variable.getName(), variable.getDescriptor(), instructionIndex);
    }

    static LocalVariable findLocalVariable(LocalVariableTable table, int index) {
        Optional<LocalVariable> o = table.getEntry(index);
        return o.orElse(null);
    }

    static void dumpToFile(String pClassName) throws ParserConfigurationException, TransformerException {
        String outPath = String.format("%s/target/jdfc", System.getProperty("user.dir"));
        File jdfcDir = new File(outPath);
        if(!jdfcDir.exists()){
            jdfcDir.mkdirs();
        }

        String filePath = String.format(outPath+"/"+pClassName+".xml");
        ClassNodeData classData = CoverageDataStore.INSTANCE.findClassDataNode(pClassName);
        TreeMap<String, List<DefUsePair>> defUsePairs = classData.getDefUsePairs();
        Map<String, Set<ProgramVariable>> defUseCovered = classData.getDefUseCovered();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element root = doc.createElement("root");
        doc.appendChild(root);

        for(Map.Entry<String, List<DefUsePair>> methodEntry: defUsePairs.entrySet()){
            if (methodEntry.getValue().size()==0){
                continue;
            }
            String methodName = methodEntry.getKey().replaceAll(":\\s","");
            methodName = methodName.replaceAll("\\(", "-");
            methodName = methodName.replaceAll("\\)", "-");
            Element method = doc.createElement(methodName);
            method.setAttribute("tagType", "method");
            root.appendChild(method);

            for(DefUsePair duPair : methodEntry.getValue()){
                NodeList methodChildren = method.getChildNodes();
                ProgramVariable variableDef = duPair.getDefinition();
                ProgramVariable variableUse = duPair.getUsage();
                Element variable = null;

                // Search for appropriate child node
                if (methodChildren.getLength() != 0){
                    for (int i = 0; i < methodChildren.getLength(); i++) {
                        if (variableDef.getName().equals(methodChildren.item(i).getNodeName())){
                            variable = (Element) methodChildren.item(i);
                            break;
                        }
                    }
                }

                // if none was found
                if (variable == null) {
                    variable = doc.createElement(variableDef.getName());
                    method.appendChild(variable);
                    variable.setAttribute("type", variableDef.getType());
                    variable.setAttribute("tagType", "variable");
                }

                // append appearance of variable
                Element appearance = doc.createElement("appearance");
                variable.appendChild(appearance);
                boolean pairCovered = defUseCovered.get(methodEntry.getKey()).contains(duPair.getDefinition())
                        && defUseCovered.get(methodEntry.getKey()).contains(duPair.getUsage());
                appearance.setAttribute("covered", Boolean.toString(pairCovered));
                appearance.setAttribute("defIndex", Integer.toString(variableDef.getInstructionIndex()));
                appearance.setAttribute("useIndex", Integer.toString(variableUse.getInstructionIndex()));
            }
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource domSource = new DOMSource(doc);
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        StreamResult streamResult = new StreamResult(file);
        transformer.transform(domSource, streamResult);
    }
}
