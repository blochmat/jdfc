package data;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import data.singleton.CoverageDataStore;
import graphs.cfg.CFG;
import graphs.cfg.LocalVariable;
import graphs.cfg.nodes.CFGNode;
import graphs.esg.ESG;
import graphs.sg.SG;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodData implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private double ratio = 0.0;

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
     * Line of method declaration in source code
     */
    private int beginLine;

    /**
     * Line of method ending in source code (including last closing bracket)
     */
    private int endLine;

    /**
     * Method declaration string from source file
     */
    private String declarationStr;

    /**
     * Local variables in class
     */
    private transient Map<Integer, LocalVariable> localVariableTable;

    /**
     * CFG of compiled method
     */
    private transient CFG cfg;

    /**
     * All program variables
     */
    private Set<UUID> pVarIds;

    /**
     * Inter-procedural SG of compiled method
     */
    private transient SG sg;

    /**
     * Inter-procedural ESG of compiled method
     */
    private transient ESG esg;

    /**
     * All DU-pairs of method
     */
    private Set<UUID> duPairIds;

    /**
     * Allocated Objects
     */
    private transient Map<Integer, Object> allocatedObjects;

    /**
     * Modified Objects
     */
    private transient Map<Integer, Object> modifiedObjects;


    public String toString() {
        return String.format("MethodData {%nAccess: %s%nName: %s%nDesc: %s%nBegin: %d%nEnd: %d%nTotal: %d%nCovered: %d%nRate: %f%nPairs: %s%n}%n",
                access, name, desc, beginLine, endLine, total, covered, ratio, JDFCUtils.prettyPrintMap(this.getDUPairsFromStore()));
    }

    // New one
    public MethodData(UUID id, String className, int access, String internalMethodName) {
        this.id = id;
        this.className = className;
        this.access = access;
        this.name = internalMethodName.split(": ")[0];
        this.desc = internalMethodName.split(": ")[1];
        this.declarationStr = "";
        this.beginLine = Integer.MIN_VALUE;
        this.endLine = Integer.MIN_VALUE;
        this.duPairIds = ConcurrentHashMap.newKeySet();
        this.localVariableTable = new HashMap<>();
        this.pVarIds = new HashSet<>();
        this.allocatedObjects = new HashMap<>();
        this.modifiedObjects = new HashMap<>();
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
        this.duPairIds = ConcurrentHashMap.newKeySet();
        this.localVariableTable = new HashMap<>();
        this.pVarIds = new HashSet<>();
        this.allocatedObjects = new HashMap<>();
        this.modifiedObjects = new HashMap<>();
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
        this.duPairIds = ConcurrentHashMap.newKeySet();
        this.localVariableTable = new HashMap<>();
        this.pVarIds = new HashSet<>();
        this.allocatedObjects = new HashMap<>();
        this.modifiedObjects = new HashMap<>();
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
        this.duPairIds = ConcurrentHashMap.newKeySet();
        this.localVariableTable = new HashMap<>();
        this.pVarIds = new HashSet<>();
        this.allocatedObjects = new HashMap<>();
        this.modifiedObjects = new HashMap<>();
    }

    public String buildInternalMethodName() {
        return String.format("%s: %s", name, desc);
    }

    public void computeCoverageMetadata() {
        this.total = duPairIds.size();
        this.covered = (int) this.getDUPairsFromStore().values().stream().filter(DefUsePair::isCovered).count();
        if (total != 0) {
            this.ratio = (double) covered / total;
        }
    }

    public UUID findVarId(ProgramVariable var) {
        for (UUID id : this.pVarIds) {
            ProgramVariable v = CoverageDataStore.getInstance().getProgramVariableMap().get(id);
            if (Objects.equals(v.getClassName(), var.getClassName())
                    && Objects.equals(v.getMethodName(), var.getMethodName())
                    && Objects.equals(v.getName(), var.getName())
                    && Objects.equals(v.getDescriptor(), var.getDescriptor())
                    && Objects.equals(v.getLineNumber(), var.getLineNumber())
                    && Objects.equals(v.getInstructionIndex(), var.getInstructionIndex())
                    && Objects.equals(v.getIsDefinition(), var.getIsDefinition())) {
                return id;
            }
        }
        return null;
    }

    public Map<UUID, ProgramVariable> getPVarsFromStore() {
        Map<UUID, ProgramVariable> pVars = new HashMap<>();
        for(UUID id: this.pVarIds) {
            pVars.put(id, CoverageDataStore.getInstance().getProgramVariableMap().get(id));
        }
        return pVars;
    }

    public Map<UUID, DefUsePair> getDUPairsFromStore() {
        Map<UUID, DefUsePair> pVars = new HashMap<>();
        for(UUID id: this.duPairIds) {
            pVars.put(id, CoverageDataStore.getInstance().getDefUsePairMap().get(id));
        }
        return pVars;
    }

    public DefUsePair findDefUsePair(DefUsePair pair) {
        for(DefUsePair p : this.getDUPairsFromStore().values()) {
            if (p.getDefId().equals(pair.getDefId()) && p.getUseId().equals(pair.getUseId())) {
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
                    if (Objects.equals(def.getName(), use.getName())
                            && Objects.equals(def.getIsField(), use.getIsField())
                            && !def.getDescriptor().equals("UNKNOWN")) {
                        UUID id = UUID.randomUUID();
                        this.duPairIds.add(id);
                        CoverageDataStore.getInstance().getDefUsePairMap()
                                .put(id, new DefUsePair(
                                        id,
                                        this.className,
                                        this.buildInternalMethodName(),
                                        def.getId(),
                                        use.getId())
                                );
                    }

                    if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                        def.setIsCovered(true);
                    }
                }
            }

        }
        JDFCUtils.logThis(this.buildInternalMethodName() + "\n" + JDFCUtils.prettyPrintMap(this.getDUPairsFromStore()), "intra_pairs");
    }

    /**
     * Checks, if a pair exists having a def or use with the specified name and line number
     * @param pName
     * @param pLineNumber
     * @return
     */
    public boolean isAnalyzedVariable(String pName, int pLineNumber) {
        for (DefUsePair pair : this.getDUPairsFromStore().values()) {
            ProgramVariable def = CoverageDataStore.getInstance().getProgramVariableMap().get(pair.getDefId());
            ProgramVariable use = CoverageDataStore.getInstance().getProgramVariableMap().get(pair.getUseId());
            if ((def.getName().equals(pName) && def.getLineNumber() == pLineNumber)
                    || use.getName().equals(pName) && use.getLineNumber() == pLineNumber) {
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
