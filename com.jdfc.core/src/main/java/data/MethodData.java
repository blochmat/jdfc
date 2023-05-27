package data;

import cfg.CFG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodData {
    private Logger logger = LoggerFactory.getLogger(MethodData.class);
    private int total = 0;
    private int covered = 0;
    private double rate = 0.0;
    private final int access;
    private final String name;
    private final String signature;
    private final List<String> exceptions;
    private final Set<DefUsePair> pairs;
    private CFG cfg;
    private Set<ProgramVariable> params;
    private int firstLine;
    private int lastLine;

    public MethodData(int access, String name, String signature, List<String> exceptions) {
        this.access = access;
        this.name = name;
        this.signature = signature;
        this.exceptions = exceptions;
        this.pairs = new HashSet<>();
    }

    /**
     * Determines if a string is the definition of this method
     *
     * @param str Single line string (without line breaks)
     * @return returns true if the provided string matches a valid method declaration
     */
    public boolean isDeclaration(String str) {
        Pattern pattern;
        if (exceptions.isEmpty()) {
            pattern = Pattern.compile(String.format("\\s*%s\\s+%s\\s+%s\\s*(\\s*%s\\s*)\\s*{",
                    JDFCUtils.getAccess(this.access),
                    JDFCUtils.getReturnType(this.signature),
                    this.name,
                    JDFCUtils.createParamPattern(this.params)));
        } else {
            pattern = Pattern.compile(String.format("\\s*%s\\s+%s\\s+%s\\s*(\\s*%s\\s*)\\s+throws\\s+%s\\s*{",
                    JDFCUtils.getAccess(this.access),
                    JDFCUtils.getReturnType(this.signature),
                    this.name,
                    JDFCUtils.createParamPattern(this.params),
                    JDFCUtils.createExceptionPattern(this.exceptions)));
        }
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    public int getTotal() {
        return total;
    }

    public int getCovered() {
        return covered;
    }

    public double getRate() {
        return rate;
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

    public Set<ProgramVariable> getParams() {
        return params;
    }

    public void setParams(Set<ProgramVariable> params) {
        this.params = params;
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

    public void computeCoverage() {
        this.total = pairs.size();
        this.covered = (int) pairs.stream().filter(DefUsePair::isCovered).count();
        if (total != 0) {
            this.rate = (double) covered / total;
        }
    }

    public DefUsePair findDefUsePair(DefUsePair pair) {
        for(DefUsePair p : pairs) {
            if (p.getDefinition().equals(pair.getDefinition()) && p.getUsage().equals(pair.getUsage())) {
                return p;
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

    public String toString() {
        return String.format("MethodData {%nMethod: %s%nTotal: %d%nCovered: %d%nRate: %f%nPairs: %s%n}%n", name, total, covered, rate, JDFCUtils.prettyPrintSet(pairs));
    }
}
