package data;

import cfg.CFG;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.Position;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class MethodData {
    @JsonIgnore
    private Logger logger = LoggerFactory.getLogger(MethodData.class);

    /**
     * Amount of DU-pairs in method
     */
    private int total = 0;

    /**
     * Amount of covered DU-pairs in method
     */
    private int covered = 0;

    /**
     * Rate between total and covered DU-pairs of method
     */
    private double rate = 0.0;

    /**
     * Access modifier of method, e.g. 0 = protected, 1 = public, 2 = private
     */
    private final int access;

    /**
     * Name of method, e.g. max
     */
    private final String name;

    /**
     * Internal signature of method, e.g. int max(int x, int y) = (II)I
     */
    private String desc;

    /**
     * Exceptions thrown by method
     */
    private String[] exceptions;

    /**
     * AST of method source code
     */
    @JsonIgnore
    private final MethodDeclaration srcAst;

    /**
     * CFG of compiled method
     */
    @JsonIgnore
    private CFG cfg;

    /**
     * All DU-pairs of method
     */
    private final Set<DefUsePair> pairs;

    /**
     * All method params as {@link ProgramVariable}
     */
    private Set<ProgramVariable> params;

    /**
     * Line of method declaration in source code
     */
    private int beginLine;

    /**
     * Line of method ending in source code (including last closing bracket)
     */
    private int endLine;

    public String toString() {
        System.err.println("Hello");
        return String.format("MethodData {%nAccess: %s%nName: %s%nDesc: %s%nExceptions: %s%nBegin: %d%nEnd: %d%nParams: %s%nTotal: %d%nCovered: %d%nRate: %f%nPairs: %s%n}%n",
                access, name, desc, Arrays.toString(exceptions), beginLine, endLine, JDFCUtils.prettyPrintSet(params), total, covered, rate, JDFCUtils.prettyPrintSet(pairs));
    }

    public MethodData(int access, String name, MethodDeclaration srcAst) {
        this.access = access;
        this.name = name;
        this.desc = JDFCUtils.toJvmDescriptor(srcAst);
        this.exceptions = JDFCUtils.toJvmExceptionDescriptor(srcAst).toArray(new String[0]);
        this.srcAst = srcAst;
        this.beginLine = extractBegin(srcAst);
        this.endLine = extractEnd(srcAst);
        this.params = new HashSet<>();
        this.pairs = new HashSet<>();
    }

    private int extractBegin(MethodDeclaration srcAst) {
        Optional<Position> posOpt = srcAst.getBegin();
        if(posOpt.isPresent()) {
            return posOpt.get().line;
        } else {
            throw new IllegalArgumentException("Method begin is undefined.");
        }
    }

    private int extractEnd(MethodDeclaration srcAst) {
        Optional<Position> posOpt = srcAst.getEnd();
        if(posOpt.isPresent()) {
            return posOpt.get().line;
        } else {
            throw new IllegalArgumentException("Method end is undefined.");
        }
    }

//    /**
//     * Determines if a string is the definition of this method
//     *
//     * @param str Single line string (without line breaks)
//     * @return returns true if the provided string matches a valid method declaration
//     */
//    public boolean isDeclaration(String str) {
//        Pattern pattern;
//        if (exceptions.isEmpty()) {
//            pattern = Pattern.compile(String.format("\\s*%s\\s+%s\\s+%s\\s*(\\s*%s\\s*)\\s*{",
//                    JDFCUtils.getAccess(this.access),
//                    JDFCUtils.getReturnType(this.signature),
//                    this.name,
//                    JDFCUtils.createParamPattern(this.params)));
//        } else {
//            pattern = Pattern.compile(String.format("\\s*%s\\s+%s\\s+%s\\s*(\\s*%s\\s*)\\s+throws\\s+%s\\s*{",
//                    JDFCUtils.getAccess(this.access),
//                    JDFCUtils.getReturnType(this.signature),
//                    this.name,
//                    JDFCUtils.createParamPattern(this.params),
//                    JDFCUtils.createExceptionPattern(this.exceptions)));
//        }
//        Matcher matcher = pattern.matcher(str);
//        return matcher.matches();
//    }

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

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public String[] getExceptions() {
        return exceptions;
    }

    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions;
    }

    public MethodDeclaration getSrcAst() {
        return srcAst;
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

    public int getBeginLine() {
        return beginLine;
    }

    public void setBeginLine(int beginLine) {
        this.beginLine = beginLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public String buildInternalMethodName() {
        System.err.println(String.format("DEBUG %s: %s", name, desc));
        return String.format("%s: %s", name, desc);
    }

    public void computeCoverage() {
        this.total = pairs.size();
        this.covered = (int) pairs.stream().filter(DefUsePair::isCovered).count();
        if (total != 0) {
            this.rate = (double) covered / total;
        }
    }

    public ProgramVariable findParamByName(String name) {
        for (ProgramVariable p : params) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
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

}
