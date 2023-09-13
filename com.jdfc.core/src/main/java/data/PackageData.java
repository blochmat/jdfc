package data;

import data.singleton.CoverageDataStore;
import lombok.Data;

import java.io.File;
import java.io.Serializable;
import java.util.*;

@Data
public class PackageData implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String relPath;
    private String fqn;
    private int total = 0;
    private int covered = 0;
    private double rate = 0.0;
    private int methodCount = 0;
    private Set<UUID> classDataIds;

    public PackageData(String relPath) {
        this.id = UUID.randomUUID();
        this.relPath = relPath;
        this.fqn = relPath.replace(File.separator, ".");
        this.classDataIds = new HashSet<>();
    }

    public Map<UUID, ClassData> getClassDataFromStore() {
        Map<UUID, ClassData> classDataMap = new HashMap<>();
        for(UUID id : classDataIds) {
            classDataMap.put(id, CoverageDataStore.getInstance().getClassDataMap().get(id));
        }
        return classDataMap;
    }

    public ClassData getClassDataByName(String name) {
        Map<UUID, ClassData> classDataMap = this.getClassDataFromStore();
        for(ClassData cData : classDataMap.values()) {
            if(cData.getName().replace(".class", "").equals(name)) {
                return cData;
            }
        }
        return null;
    }
}
