package data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.Position;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import graphs.cfg.CFG;
import graphs.cfg.LocalVariable;
import graphs.cfg.nodes.CFGNode;
import graphs.esg.ESG;
import graphs.sg.SG;
import graphs.sg.nodes.SGNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodData {

    private UUID id;

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
     * Name of containing class
     */
    private String className;

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
     * CFG of compiled method
     */
    @JsonIgnore
    private CFG cfg;

    /**
     * Inter-procedural SG of compiled method
     */
    @JsonIgnore
    private SG sg;

    /**
     * Inter-procedural ESG of compiled method
     */
    @JsonIgnore
    private ESG esg;

    /**
     * Local variables in class
     */
    private Map<Integer, LocalVariable> localVariableTable;

    /**
     * All DU-pairs of method
     */
    private Set<DefUsePair> pairs;

    /**
     * All program variables
     */
    private Map<UUID, ProgramVariable> programVariables;

    /**
     * Line of method declaration in source code
     */
    private int beginLine;

    /**
     * Line of method ending in source code (including last closing bracket)
     */
    private int endLine;

    public String toString() {
        return String.format("MethodData {%nAccess: %s%nName: %s%nDesc: %s%nBegin: %d%nEnd: %d%nTotal: %d%nCovered: %d%nRate: %f%nPairs: %s%n}%n",
                access, name, desc, beginLine, endLine, total, covered, rate, JDFCUtils.prettyPrintSet(pairs));
    }

    public MethodData(UUID id, String classname, int access, String name, String desc) {
        this.id = id;
        this.className = classname;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.declarationStr = "";
        this.beginLine = Integer.MIN_VALUE;
        this.endLine = Integer.MIN_VALUE;
        this.pairs = ConcurrentHashMap.newKeySet();
        this.localVariableTable = new HashMap<>();
        this.programVariables = new HashMap<>();
    }

    public MethodData(UUID id, String className, int access, String name, String desc, MethodDeclaration srcAst) {
        this.id = id;
        this.className = className;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.declarationStr = srcAst.getDeclarationAsString();
        this.beginLine = extractBegin(srcAst);
        this.endLine = extractEnd(srcAst);
        this.pairs = ConcurrentHashMap.newKeySet();
        this.localVariableTable = new HashMap<>();
        this.programVariables = new HashMap<>();
    }

    public MethodData(UUID id, String className, int access, String name, String desc, ConstructorDeclaration srcAst) {
        this.id = id;
        this.className = className;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.declarationStr = srcAst.getDeclarationAsString();
        this.beginLine = extractBegin(srcAst);
        this.endLine = extractEnd(srcAst);
        this.pairs = ConcurrentHashMap.newKeySet();
        this.localVariableTable = new HashMap<>();
        this.programVariables = new HashMap<>();
    }

    public String buildInternalMethodName() {
        return String.format("%s: %s", name, desc);
    }

    public void computeCoverage() {
        this.total = pairs.size();
        this.covered = (int) pairs.stream().filter(DefUsePair::isCovered).count();
        if (total != 0) {
            this.rate = (double) covered / total;
        }
    }

    public UUID findVarId(ProgramVariable var) {
        for (Map.Entry<UUID,ProgramVariable> entry : programVariables.entrySet()) {
            ProgramVariable v = entry.getValue();
            if (Objects.equals(v.getClassName(), var.getClassName())
                    && Objects.equals(v.getMethodName(), var.getMethodName())
                    && Objects.equals(v.getName(), var.getName())
                    && Objects.equals(v.getDescriptor(), var.getDescriptor())
                    && Objects.equals(v.getLineNumber(), var.getLineNumber())
                    && Objects.equals(v.getInstructionIndex(), var.getInstructionIndex())
                    && Objects.equals(v.getIsDefinition(), var.getIsDefinition())) {
                return entry.getKey();
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

    /**
     * Calculates intra Def-Use-Pairs.
     */
    public void calculateIntraDefUsePairs() {
        for (Map.Entry<Integer, CFGNode> entry : this.cfg.getNodes().entrySet()) {
            CFGNode node = entry.getValue();

            for (ProgramVariable def : node.getReach()) {
                for (ProgramVariable use : node.getUses()) {
                    if (def.getName().equals(use.getName()) && !def.getDescriptor().equals("UNKNOWN")) {
                        this.pairs.add(new DefUsePair(def, use));
                    }
                    if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                        def.setIsCovered(true);
                    }
                }
            }

        }
        JDFCUtils.logThis(this.buildInternalMethodName() + "\n" + JDFCUtils.prettyPrintSet(this.pairs), "intra_pairs");
    }

    /**
     * Calculates inter Def-Use-Pairs.
     */
    public void calculateInterDefUsePairs() {
        // TODO: Workaround, because set contains duplicates due to currently unknown reasons
        this.pairs.clear();

        for (Map.Entry<Integer, SGNode> entry : this.sg.getNodes().entrySet()) {
            SGNode node = entry.getValue();

            for (ProgramVariable def : node.getReach()) {
                for (ProgramVariable use : node.getUses()) {
                    if (Objects.equals(def.getName(), use.getName())
                            && Objects.equals(def.getDescriptor(), use.getDescriptor())
                            && !Objects.equals(def.getDescriptor(), "UNKNOWN")) {
                        this.pairs.add(new DefUsePair(def, use));
                    }
                    if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                        def.setIsCovered(true);
                    }
                }
            }

        }
        JDFCUtils.logThis(this.buildInternalMethodName() + "\n" + JDFCUtils.prettyPrintSet(this.pairs), "inter_pairs");
    }

    /**
     * Checks, if a pair exists having a def or use with the specified name and line number
     * @param pName
     * @param pLineNumber
     * @return
     */
    public boolean isAnalyzedVariable(String pName, int pLineNumber) {
        for (DefUsePair pair : pairs) {
            if ((pair.getDefinition().getName().equals(pName) && pair.getDefinition().getLineNumber() == pLineNumber)
                    || pair.getUsage().getName().equals(pName) && pair.getUsage().getLineNumber() == pLineNumber) {
                return true;
            }
        }
        return false;
    }


    // --- Private Methods ---------------------------------------------------------------------------------------------
    private int extractBegin(MethodDeclaration srcAst) {
        Optional<Position> posOpt = srcAst.getBegin();
        if(posOpt.isPresent()) {
            return posOpt.get().line;
        } else {
            throw new IllegalArgumentException("Method begin is undefined.");
        }
    }

    private int extractBegin(ConstructorDeclaration srcAst) {
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

    private int extractEnd(ConstructorDeclaration srcAst) {
        Optional<Position> posOpt = srcAst.getEnd();
        if(posOpt.isPresent()) {
            return posOpt.get().line;
        } else {
            throw new IllegalArgumentException("Method end is undefined.");
        }
    }
}
