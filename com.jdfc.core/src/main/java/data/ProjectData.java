package data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProjectData {

    private final Map<UUID, ProgramVariable> programVariableMap;
    private final Map<String, Set<UUID>> testRunMap;
    private final Map<UUID, DefUsePair> defUsePairMap;

    public Map<UUID, ProgramVariable> getProgramVariableMap() {
        return programVariableMap;
    }

    public Map<String, Set<UUID>> getTestRunMap() {
        return testRunMap;
    }

    public Map<UUID, DefUsePair> getDefUsePairMap() {
        return defUsePairMap;
    }

    public ProjectData() {
        programVariableMap = new HashMap<>();
        testRunMap = new HashMap<>();
        defUsePairMap = new HashMap<>();
    }
}
