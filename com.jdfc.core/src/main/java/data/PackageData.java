package data;

import lombok.Data;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Data
public class PackageData implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String relPath;
    private String fqn;
    private int total = 0;
    private int covered = 0;
    private double ratio = 0.0;
    private int methodCount = 0;
    private Set<UUID> classDataIds;

    public PackageData(String relPath) {
        this.id = UUID.randomUUID();
        this.relPath = relPath;
        this.fqn = relPath.replace(File.separator, ".").substring(1);
        this.classDataIds = new HashSet<>();
    }

    public Map<UUID, ClassData> getClassDataFromStore() {
        Map<UUID, ClassData> classDataMap = new HashMap<>();
        for(UUID id : classDataIds) {
            classDataMap.put(id, ProjectData.getInstance().getClassDataMap().get(id));
        }
        return classDataMap;
    }

    // Serialization
//    private void writeObject(ObjectOutputStream out) throws IOException {
//        out.writeLong(id.getMostSignificantBits());
//        out.writeLong(id.getLeastSignificantBits());
//        writeString(out, relPath);
//        writeString(out, fqn);
//        out.writeInt(total);
//        out.writeInt(covered);
//        out.writeDouble(ratio);
//        out.writeInt(methodCount);
//        writeUUIDSet(out, classDataIds);
//    }
//
//    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//        long mostSigBits = in.readLong();
//        long leastSigBits = in.readLong();
//        id = new UUID(mostSigBits, leastSigBits);
//        relPath = readString(in);
//        fqn = readString(in);
//        total = in.readInt();
//        covered = in.readInt();
//        ratio = in.readDouble();
//        methodCount = in.readInt();
//        classDataIds = readUUIDSet(in);
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
}
