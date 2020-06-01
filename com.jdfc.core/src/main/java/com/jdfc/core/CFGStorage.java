package main.java.com.jdfc.core;

import main.java.com.jdfc.core.cfg.CFG;
import main.java.com.jdfc.core.cfg.CFGNode;
import main.java.com.jdfc.core.cfg.DefUsePair;
import main.java.com.jdfc.core.cfg.ProgramVariable;

import java.util.*;
import java.util.stream.Collectors;

/** A storage singleton for method {@link CFG}s. */
public enum CFGStorage {
    INSTANCE;

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

    public void setDefUsePairs(TreeMap<String, List<DefUsePair>> defUsePairs) {
        this.defUsePairs = defUsePairs;
    }

    public void setDefUseCovered(Map<String, Set<ProgramVariable>> defUseCovered) {
        this.defUseCovered = defUseCovered;
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
