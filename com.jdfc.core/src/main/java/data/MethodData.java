package data;

import java.util.HashSet;
import java.util.Set;

public class MethodData {
    private int total = 0;
    private int covered = 0;
    private double rate = 0.0;
    private final String name;
    private final String signature;
    private final Set<DefUsePair> pairs;

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

    public void setName(String name) {
        this.name = name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Set<DefUsePair> getPairs() {
        return pairs;
    }

    public void setPairs(Set<DefUsePair> pairs) {
        this.pairs = pairs;
    }
}
