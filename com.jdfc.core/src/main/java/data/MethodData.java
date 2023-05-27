package data;

import cfg.CFG;

import java.util.HashSet;
import java.util.Set;

public class MethodData {
    private int total = 0;
    private int covered = 0;
    private double rate = 0.0;
    private final String name;
    private final String signature;
    private final Set<DefUsePair> pairs;
    private CFG cfg;
    private int firstLine;
    private int lastLine;

    public MethodData(String name, String signature) {
        this.name = name;
        this.signature = signature;
        this.pairs = new HashSet<>();
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCovered() {
        return covered;
    }

    public void setCovered(int covered) {
        this.covered = covered;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public Set<DefUsePair> getPairs() {
        return pairs;
    }

    public CFG getCfg() {
        return cfg;
    }

    public void setCfg(CFG cfg) {
        this.cfg = cfg;
    }

    public int getFirstLine() {
        return firstLine;
    }

    public void setFirstLine(int firstLine) {
        this.firstLine = firstLine;
    }

    public int getLastLine() {
        return lastLine;
    }

    public void setLastLine(int lastLine) {
        this.lastLine = lastLine;
    }

    public DefUsePair findDefUsePair(DefUsePair pair) {
        for(DefUsePair p : pairs) {
            if (p.getDefinition().equals(pair.getDefinition()) && p.getUsage().equals(pair.getUsage())) {
                return pair;
            }
        }
        return null;
    }

    public DefUsePair findDefUsePair(ProgramVariable def, ProgramVariable use) {
        for(DefUsePair pair : pairs) {
            if (pair.getDefinition().equals(def) && pair.getUsage().equals(use)) {
                return pair;
            }
        }
        return null;
    }
}