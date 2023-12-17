package data;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import instr.ClassMetaData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ClassData implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient ClassOrInterfaceDeclaration ciDecl;

    private UUID id;

    private ClassMetaData classMetaData;

    private Map<String, String> nestedTypeMap;

    private Set<UUID> methodDataIds;

    private Map<Integer, UUID> lineToMethodIdMap;

    private int total = 0;

    private int covered = 0;

    private double ratio = 0.0;

    private int methodCount = 0;

    public ClassData(UUID id, ClassMetaData classMetaData, Map<String, String> nestedTypeMap) {
        this.id = id;
        this.classMetaData = classMetaData;
        this.nestedTypeMap = nestedTypeMap;
        this.methodDataIds = new HashSet<>();
        this.lineToMethodIdMap = new HashMap<>();
    }

    public Map<UUID, MethodData> getMethodDataFromStore() {
        Map<UUID, MethodData> methodDataMap = new HashMap<>();
        for (UUID id : this.methodDataIds) {
            methodDataMap.put(id, ProjectData.getInstance().getMethodDataMap().get(id));
        }
        return methodDataMap;
    }

    public MethodData getMethodByInternalName(String internalName) {
        for(MethodData mData : this.getMethodDataFromStore().values()) {
            if (mData.buildInternalMethodName().equals(internalName)) {
                return mData;
            }
        }

        return null;
    }

    public MethodData getMethodByShortInternalName(String internalName) {
        for(MethodData mData : this.getMethodDataFromStore().values()) {
            if (mData.buildInternalMethodName().contains(internalName)) {
                return mData;
            }
        }

        return null;
    }

    public MethodData getMethodByLineNumber(int lNr) {
        for(MethodData mData : this.getMethodDataFromStore().values()) {
            if (mData.getBeginLine() <= lNr && lNr <= mData.getEndLine()) {
                return mData;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ClassData{" +
//                "srcFileAst=" + srcFileAst +
//                ", pkgDecl=" + pkgDecl +
//                ", impDeclList=" + impDeclList +
                ", ciDecl=" + ciDecl +
                ", id=" + id +
                ", nestedTypeMap=" + nestedTypeMap +
                ", methodDataIds=" + methodDataIds +
                ", lineToMethodIdMap=" + lineToMethodIdMap +
//                ", fieldDefinitions=" + fieldDefinitions +
                ", total=" + total +
                ", covered=" + covered +
                ", rate=" + ratio +
                ", methodCount=" + methodCount +
                '}';
    }

    // Serialization
//    private void writeObject(ObjectOutputStream out) throws IOException {
//        out.writeLong(id.getMostSignificantBits());
//        out.writeLong(id.getLeastSignificantBits());
//        out.writeObject(classMetaData);
//        writeMap(out, nestedTypeMap);
//        writeUUIDSet(out, methodDataIds);
//        writeLineToMethodIdMap(out, lineToMethodIdMap);
//        out.writeInt(total);
//        out.writeInt(covered);
//        out.writeDouble(ratio);
//        out.writeInt(methodCount);
//    }
//
//    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//        long mostSigBits = in.readLong();
//        long leastSigBits = in.readLong();
//        id = new UUID(mostSigBits, leastSigBits);
//        classMetaData = (ClassMetaData) in.readObject();
//        nestedTypeMap = readMap(in);
//        methodDataIds = readUUIDSet(in);
//        lineToMethodIdMap = readLineToMethodIdMap(in);
//        total = in.readInt();
//        covered = in.readInt();
//        ratio = in.readDouble();
//        methodCount = in.readInt();
//    }
//
//    private void writeMap(ObjectOutputStream out, Map<String, String> map) throws IOException {
//        out.writeInt(map.size());
//        for (Map.Entry<String, String> entry : map.entrySet()) {
//            writeString(out, entry.getKey());
//            writeString(out, entry.getValue());
//        }
//    }
//
//    private Map<String, String> readMap(ObjectInputStream in) throws IOException {
//        int size = in.readInt();
//        Map<String, String> map = new HashMap<>();
//        for (int i = 0; i < size; i++) {
//            String key = readString(in);
//            String value = readString(in);
//            map.put(key, value);
//        }
//        return map;
//    }
//
//    private void writeString(ObjectOutputStream out, String str) throws IOException {
//        byte[] bytes = str != null ? str.getBytes(StandardCharsets.UTF_8) : new byte[0];
//        out.writeInt(bytes.length);
//        out.write(bytes);
//    }
//
//    private String readString(ObjectInputStream in) throws IOException {
//        int length = in.readInt();
//        if (length == 0) return "";
//        byte[] bytes = new byte[length];
//        in.readFully(bytes);
//        return new String(bytes, StandardCharsets.UTF_8);
//    }
//
//    private void writeUUIDSet(ObjectOutputStream out, Set<UUID> uuidSet) throws IOException {
//        out.writeInt(uuidSet.size());
//        for (UUID uuid : uuidSet) {
//            out.writeLong(uuid.getMostSignificantBits());
//            out.writeLong(uuid.getLeastSignificantBits());
//        }
//    }
//
//    private Set<UUID> readUUIDSet(ObjectInputStream in) throws IOException {
//        int size = in.readInt();
//        Set<UUID> uuidSet = new HashSet<>();
//        for (int i = 0; i < size; i++) {
//            long mostSigBits = in.readLong();
//            long leastSigBits = in.readLong();
//            uuidSet.add(new UUID(mostSigBits, leastSigBits));
//        }
//        return uuidSet;
//    }
//
//    private void writeLineToMethodIdMap(ObjectOutputStream out, Map<Integer, UUID> map) throws IOException {
//        out.writeInt(map.size());
//        for (Map.Entry<Integer, UUID> entry : map.entrySet()) {
//            out.writeInt(entry.getKey());
//            out.writeLong(entry.getValue().getMostSignificantBits());
//            out.writeLong(entry.getValue().getLeastSignificantBits());
//        }
//    }
//
//    private Map<Integer, UUID> readLineToMethodIdMap(ObjectInputStream in) throws IOException {
//        int size = in.readInt();
//        Map<Integer, UUID> map = new HashMap<>();
//        for (int i = 0; i < size; i++) {
//            int key = in.readInt();
//            long mostSigBits = in.readLong();
//            long leastSigBits = in.readLong();
//            map.put(key, new UUID(mostSigBits, leastSigBits));
//        }
//        return map;
//    }
}
