package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.Position;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import data.singleton.CoverageDataStore;
import graphs.cfg.CFG;
import graphs.cfg.nodes.CFGNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.util.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private int access;

    /**
     * Name of method, e.g. max
     */
    private String name;

    /**
     * Internal signature of method, e.g. int max(int x, int y) = (II)I
     */
    private String desc;

    /**
     * Method declaration string from source file
     */
    private String declarationStr;

    /**
     * AST of method source code
     */
    @JsonIgnore
    private MethodDeclaration srcAst;

    /**
     * CFG of compiled method
     */
    @JsonIgnore
    private CFG cfg;

    /**
     * All DU-pairs of method
     */
    private Set<DefUsePair> pairs;

    /**
     * Local variables in class
     */
    private Map<Integer, UUID> localVarIdxToUUID;

    /**
     * All program variables
     */
    private Multimap<Integer, UUID> programVarLineToUUID;

    /**
     * All method params as {@link ProgramVariable}
     */
    private Set<UUID> params;

    /**
     * Line of method declaration in source code
     */
    private int beginLine;

    /**
     * Line of method ending in source code (including last closing bracket)
     */
    private int endLine;

    public String toString() {
        return String.format("%nMethodData {%nAccess: %s%nName: %s%nDesc: %s%nBegin: %d%nEnd: %d%nParams: %s%nTotal: %d%nCovered: %d%nRate: %f%nPairs: %s%n}%n",
                access, name, desc, beginLine, endLine, JDFCUtils.prettyPrintSet(params), total, covered, rate, JDFCUtils.prettyPrintSet(pairs));
    }

    public MethodData(int access, String name, String desc, MethodDeclaration srcAst) {
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.declarationStr = srcAst.getDeclarationAsString();
        this.srcAst = srcAst;
        this.beginLine = extractBegin(srcAst);
        this.endLine = extractEnd(srcAst);
        this.params = new HashSet<>();
        this.pairs = new HashSet<>();
        this.programVarLineToUUID = ArrayListMultimap.create();
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

    public String buildInternalMethodName() {
        String internalName = String.format("%s: %s", name, desc);
        logger.debug(String.format("buildInternalMethodName = %s", internalName));
        return internalName;
    }

    public void computeCoverage() {
        this.total = pairs.size();
        this.covered = (int) pairs.stream().filter(DefUsePair::isCovered).count();
        if (total != 0) {
            this.rate = (double) covered / total;
        }
    }

//    public ProgramVariable findVar(ProgramVariable var) {
//        logger.debug(String.format("findVar(%s)", var));
//        for (ProgramVariable v : programVarLineToUUID) {
//            if (Objects.equals(v.getOwner(), var.getOwner())
//                    && Objects.equals(v.getName(), var.getName())
//                    && Objects.equals(v.getDesc(), var.getDesc())
//                    && Objects.equals(v.getLineNr(), var.getLineNr())
//                    && Objects.equals(v.getInsnIdx(), var.getInsnIdx())
//                    && Objects.equals(v.isDef(), var.isDef())) {
//                logger.debug(String.format("- %s", v));
//                return v;
//            }
//        }
//        logger.debug("Return NULL");
//        return null;
//    }

    public DefUsePair findDefUsePair(DefUsePair pair) {
        for(DefUsePair p : pairs) {
            if (p.getDefinition().equals(pair.getDefinition()) && p.getUsage().equals(pair.getUsage())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Calculates all possible Def-Use-Pairs.
     */
    public void calculateDefUsePairs() {
        logger.debug("calculateDefUsePairs");

        CoverageDataStore store = CoverageDataStore.getInstance();
        for (Map.Entry<Double, CFGNode> entry : this.cfg.getNodes().entrySet()) {
            CFGNode node = entry.getValue();

            logger.debug(JDFCUtils.prettyPrintSet(node.getReach()));
            for (UUID defID : node.getReach()) {
                for (UUID useID : node.getUses()) {
                    ProgramVariable def = store.getUuidProgramVariableMap().get(defID);
                    ProgramVariable use = store.getUuidProgramVariableMap().get(useID);
                    if (def.getName().equals(use.getName()) && !def.getDesc().equals("UNKNOWN")) {
                        this.pairs.add(new DefUsePair(def, use));
                    }
                    if (def.getInsnIdx() == Integer.MIN_VALUE) {
                        def.setCov(true);
                    }
                }
            }
        }
        logger.debug(JDFCUtils.prettyPrintSet(this.pairs));
    }

    /**
     * Checks, if a pair exists having a def or use with the specified name and line number
     * @param pName
     * @param pLineNumber
     * @return
     */
    public boolean isAnalyzedVariable(String pName, int pLineNumber) {
        for (DefUsePair pair : pairs) {
            if ((pair.getDefinition().getName().equals(pName) && pair.getDefinition().getLineNr() == pLineNumber)
                    || pair.getUsage().getName().equals(pName) && pair.getUsage().getLineNr() == pLineNumber) {
                return true;
            }
        }
        return false;
    }
}
