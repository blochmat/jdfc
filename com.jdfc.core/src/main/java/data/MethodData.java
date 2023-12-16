package data;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import graphs.cfg.CFG;
import graphs.cfg.LocalVariable;
import graphs.cfg.nodes.CFGCallNode;
import graphs.cfg.nodes.CFGNode;
import graphs.esg.ESG;
import graphs.sg.SG;
import graphs.sg.nodes.SGNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static utils.Constants.ZERO_ID;

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
        this.covered = (int) this.getDUPairsFromStore().values().stream().filter(PairData::isCovered).count();
        if (total != 0) {
            this.ratio = (double) covered / total;
        }
    }

    public UUID findVarId(ProgramVariable var) {
        for (UUID id : this.pVarIds) {
            ProgramVariable v = ProjectData.getInstance().getProgramVariableMap().get(id);
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
            pVars.put(id, ProjectData.getInstance().getProgramVariableMap().get(id));
        }
        return pVars;
    }

    public Map<UUID, PairData> getDUPairsFromStore() {
        Map<UUID, PairData> pVars = new HashMap<>();
        for(UUID id: this.duPairIds) {
            pVars.put(id, ProjectData.getInstance().getDefUsePairMap().get(id));
        }
        return pVars;
    }

    public PairData findDefUsePair(PairData pair) {
        for(PairData p : this.getDUPairsFromStore().values()) {
            if (p.getDefId().equals(pair.getDefId()) && p.getUseId().equals(pair.getUseId())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Calculates intra Def-Use-Pairs.
     */
    public void calculateIntraProcDefUsePairs() {
        for (Map.Entry<Integer, CFGNode> entry : this.cfg.getNodes().entrySet()) {
            CFGNode node = entry.getValue();
            for (ProgramVariable def : node.getReach()) {
                for (ProgramVariable use : node.getUses()) {
                    if (Objects.equals(def.getName(), use.getName())
                            && Objects.equals(def.getIsField(), use.getIsField())
                            && !def.getDescriptor().equals("UNKNOWN")) {
                        createDefUsePair(def, use);
                    }

                    if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                        def.setIsCovered(true);
                    }
                }
            }

        }
        JDFCUtils.logThis(this.buildInternalMethodName() + "\n" + JDFCUtils.prettyPrintMap(this.getDUPairsFromStore()), "intra_pairs");
    }

    private void createDefUsePair(ProgramVariable def, ProgramVariable use) {
        Map<UUID, PairData> pairs = ProjectData.getInstance().getDefUsePairMap();
        UUID id = UUID.randomUUID();
        PairData newPair = new PairData(id, this.className, this.buildInternalMethodName(), def.getId(), use.getId());
        if(!pairs.containsValue(newPair)) {
            this.duPairIds.add(id);
            pairs.put(id, newPair);
            if (def.getInstructionIndex() == Integer.MIN_VALUE) {
                def.setIsCovered(true);
            }

        }
    }

    public void calculateInterProcDefUsePairs(Map<Integer, Set<UUID>> MVP) {
        for (Map.Entry<Integer, SGNode>  sgNodeEntry : this.getSg().getNodes().entrySet()) {
            int sgNodeIdx = sgNodeEntry.getKey();
            SGNode sgNode = sgNodeEntry.getValue();
            Set<UUID> reachingDefs = MVP.get(sgNodeIdx);
            if (!sgNode.getUses().isEmpty()) {
                for (ProgramVariable use : sgNode.getUses()) {
                    for (UUID intraDefId : reachingDefs) {
                        if (intraDefId == ZERO_ID) {
                            continue;
                        }
                        // Intra
                        ProgramVariable intraDef = ProjectData.getInstance().getProgramVariableMap().get(intraDefId);
                        if (!use.getDescriptor().equals("UNKNOWN")
                                && !intraDef.getDescriptor().equals("UNKNOWN")
                                && use.isIntraProcUseOf(intraDef)) {
                            createDefUsePair(intraDef, use);
                            // Inter
                            for (UUID interDefId : reachingDefs) {
                                if (intraDef.isMatchOf(interDefId)) {
                                    ProgramVariable interDef = ProjectData.getInstance().getProgramVariableMap().get(interDefId);
                                    createDefUsePair(interDef, use);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks, if a pair exists having a def or use with the specified name and line number
     * @param pName
     * @param pLineNumber
     * @return
     */
    public boolean isAnalyzedVariable(String pName, int pLineNumber) {
        for (PairData pair : this.getDUPairsFromStore().values()) {
            ProgramVariable def = ProjectData.getInstance().getProgramVariableMap().get(pair.getDefId());
            ProgramVariable use = ProjectData.getInstance().getProgramVariableMap().get(pair.getUseId());
            if ((def.getName().equals(pName) && def.getLineNumber() == pLineNumber)
                    || use.getName().equals(pName) && use.getLineNumber() == pLineNumber) {
                return true;
            }
        }
        return false;
    }

    public void createIndexDefinitionsMap() {
        for (Map.Entry<Integer, CFGNode> entry : this.cfg.getNodes().entrySet()) {
            CFGNode node = entry.getValue();

            if (node instanceof CFGCallNode) {
                Multimap<Integer, ProgramVariable> indexDefinitionsMap = ArrayListMultimap.create();
                CFGCallNode cfgCallNode = (CFGCallNode) node;
                for (Map.Entry<Integer, ProgramVariable> paramEntry : cfgCallNode.getIndexUseMap().entrySet()) {
                    ProgramVariable usage = paramEntry.getValue();
                    Set<PairData> duPairs = ProjectData.getInstance().getDefUsePairMap().values()
                            .stream()
                            .filter(x -> x.getUseId().equals(usage.getId()))
                            .collect(Collectors.toSet());
                    Set<UUID> defIds = duPairs.stream().map(PairData::getDefId).collect(Collectors.toSet());
                    for (UUID defId : defIds) {
                        indexDefinitionsMap.put(paramEntry.getKey(), ProjectData.getInstance().getProgramVariableMap().get(defId));
                    }
                }
                cfgCallNode.setIndexDefinitionsMap(indexDefinitionsMap);
            }
        }
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

    // Serialization
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
        out.writeInt(total);
        out.writeInt(covered);
        out.writeDouble(ratio);
        out.writeInt(access);
        writeString(out, className);
        writeString(out, name);
        writeString(out, desc);
        out.writeInt(beginLine);
        out.writeInt(endLine);
        writeString(out, declarationStr);
        // Note: Transient fields are not serialized

        // Serialize UUID Sets
        writeUUIDSet(out, pVarIds);
        writeUUIDSet(out, duPairIds);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        long mostSigBits = in.readLong();
        long leastSigBits = in.readLong();
        id = new UUID(mostSigBits, leastSigBits);
        total = in.readInt();
        covered = in.readInt();
        ratio = in.readDouble();
        access = in.readInt();
        className = readString(in);
        name = readString(in);
        desc = readString(in);
        beginLine = in.readInt();
        endLine = in.readInt();
        declarationStr = readString(in);
        // Note: Transient fields are not deserialized

        // Deserialize UUID Sets
        pVarIds = readUUIDSet(in);
        duPairIds = readUUIDSet(in);
    }

    private void writeString(ObjectOutputStream out, String str) throws IOException {
        byte[] bytes = str != null ? str.getBytes(StandardCharsets.UTF_8) : new byte[0];
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private String readString(ObjectInputStream in) throws IOException {
        int length = in.readInt();
        if (length == 0) return "";
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeUUIDSet(ObjectOutputStream out, Set<UUID> uuidSet) throws IOException {
        out.writeInt(uuidSet.size());
        for (UUID uuid : uuidSet) {
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());
        }
    }

    private Set<UUID> readUUIDSet(ObjectInputStream in) throws IOException {
        int size = in.readInt();
        Set<UUID> uuidSet = new HashSet<>();
        for (int i = 0; i < size; i++) {
            long mostSigBits = in.readLong();
            long leastSigBits = in.readLong();
            uuidSet.add(new UUID(mostSigBits, leastSigBits));
        }
        return uuidSet;
    }
}
