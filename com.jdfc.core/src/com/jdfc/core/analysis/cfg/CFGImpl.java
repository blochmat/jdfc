package com.jdfc.core.analysis.cfg;


import com.google.common.base.Preconditions;
import com.jdfc.commons.internal.PrettyPrintMap;
import com.jdfc.core.analysis.CFGStorage;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        dumpToFile();
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

    static void dumpToFile() {
        String outPath = String.format("%s/target/output.xml", System.getProperty("user.dir"));

        TreeMap<String, List<DefUsePair>> defUsePairs = CFGStorage.INSTANCE.getDefUsePairs();
        Map<String, Set<ProgramVariable>> defUseCovered = CFGStorage.INSTANCE.getDefUseCovered();

        Element root = new Element("root");
        Document doc = new Document(root);

        for (Map.Entry<String, List<DefUsePair>> methodEntry : defUsePairs.entrySet()) {
            if (methodEntry.getValue().size() == 0) {
                continue;
            }
            Element method = new Element("method");
            Attribute methodName = new Attribute("name", methodEntry.getKey());
            Attribute totalPairs = new Attribute("total", Integer.toString(methodEntry.getValue().size()));
            method.setAttribute(methodName);
            method.setAttribute(totalPairs);

            Element defUsePairList = new Element("defUsePairs");

            for (DefUsePair duPair : methodEntry.getValue()) {
                ProgramVariable variableDef = duPair.getDefinition();
                ProgramVariable variableUse = duPair.getUsage();

                Element pair = new Element("pair");
                boolean pairCovered = defUseCovered.get(methodEntry.getKey()).contains(duPair.getDefinition())
                        && defUseCovered.get(methodEntry.getKey()).contains(duPair.getUsage());
                Attribute covered = new Attribute("covered", Boolean.toString(pairCovered));
                pair.setAttribute(covered);

                Element definition = new Element("definition");
                addVariableAttributesToElement(variableDef, definition);
                Element use = new Element("use");
                addVariableAttributesToElement(variableUse, use);

                pair.addContent(definition);
                pair.addContent(use);

                defUsePairList.addContent(pair);
            }
            method.addContent(defUsePairList);
            root.addContent(method);
        }

        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat());
        try {
            xmlOutputter.output(doc, new FileWriter(new File(outPath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addVariableAttributesToElement(ProgramVariable var, Element element) {
        Attribute defName = new Attribute("name", var.getName());
        Attribute defType = new Attribute("type", var.getType());
        Attribute defIndex = new Attribute("insnIndex", Integer.toString(var.getInstructionIndex()));
        element.setAttribute(defName);
        element.setAttribute(defType);
        element.setAttribute(defIndex);
    }

}
