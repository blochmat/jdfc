package com.jdfc.core.analysis.internal.data;

import com.jdfc.commons.data.NodeData;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.core.analysis.cfg.CFGNode;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.cfg.ProgramVariable;

import java.util.*;
import java.util.stream.Collectors;

public class ClassNodeData extends NodeData {

    private Map<String, CFG> methodCFGs;
    private TreeMap<String, List<DefUsePair>> defUsePairs;
    private Map<String, Set<ProgramVariable>> defUseCovered;

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
