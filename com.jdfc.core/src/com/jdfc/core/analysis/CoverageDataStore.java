package com.jdfc.core.analysis;

import com.jdfc.commons.data.Node;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.core.analysis.cfg.CFGNode;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.ProgramVariable;
import com.jdfc.commons.data.NodeData;

import java.util.*;
import java.util.stream.Collectors;

/** A storage singleton for package, class and finally method {@link CFG}s. */
public enum CoverageDataStore {
    INSTANCE;

    private Node<NodeData> root;
    private List<String> classList;
    private Map<String, CFG> methodCFGs;
    private TreeMap<String, List<DefUsePair>> defUsePairs;
    private Map<String, Set<ProgramVariable>> defUseCovered;

    public Node<NodeData> getRoot() {
        return root;
    }
    public void setRoot(Node<NodeData> root) {
        this.root = root;
    }

    public List<String> getClassList(){
        return classList;
    }

    public void setClassList(List<String> pClassList){
        this.classList = pClassList;
    }

    /**
     * Sets the method {@link CFG}s.
     *
     * @param pMethodCFGs The mapping of method names and {@link CFG}s
     */
    public void setMethodCFGs(final Map<String, CFG> pMethodCFGs) {
        methodCFGs = pMethodCFGs;
    }

    /**
     * Returns the mapping of method names and {@link CFG}s.
     *
     * @return The mapping of method names and {@link CFG}s
     */
    public Map<String, CFG> getMethodCFGs() {
        return methodCFGs;
    }

    public TreeMap<String, List<DefUsePair>> getDefUsePairs() {
        return defUsePairs;
    }

    public Map<String, Set<ProgramVariable>> getDefUseCovered() {
        return defUseCovered;
    }

    /** Calculates all possible Def-Use-Pairs. */
    public void calculateDefUsePairs() {
        defUsePairs = new TreeMap<>();
        defUseCovered = new HashMap<>();
        for (CFG graph : methodCFGs.values()) {
            String name =
                    methodCFGs
                            .entrySet()
                            .stream()
                            .filter(stringCFGEntry -> stringCFGEntry.getValue().equals(graph))
                            .collect(Collectors.toList())
                            .get(0)
                            .getKey();
            defUsePairs.put(name, new ArrayList<>());
            defUseCovered.put(name, new HashSet<>());
            for (CFGNode node : graph.getNodes().values()) {
                for (ProgramVariable def : node.getReach()) {
                    for (ProgramVariable use : node.getUses()) {
                        if (def.getName().equals(use.getName())) {
                            defUsePairs.get(name).add(new DefUsePair(def, use));
                            if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                                defUseCovered.get(name).add(def);
                            }
                        }
                    }
                }
            }
        }
    }
}
