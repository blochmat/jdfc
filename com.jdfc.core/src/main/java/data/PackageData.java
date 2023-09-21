package data;

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
}
