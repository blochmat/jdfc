package data.neu;

import data.DefUsePair;
import data.ProgramVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProjectData {
    private final Map<UUID, String> classNameMap;
    private final Map<UUID, String> methodNameMap;
    private final Map<UUID, ProgramVariable> programVariableMap;
    private final Map<UUID, DefUsePair> defUsePairMap;
    private final Map<String, Set<UUID>> testRunMap;

    public ProjectData() {
        classNameMap = new HashMap<>();
        methodNameMap = new HashMap<>();
        programVariableMap = new HashMap<>();
        defUsePairMap = new HashMap<>();
        testRunMap = new HashMap<>();
    }

    public Map<UUID, String> getClassNameMap() {
        return classNameMap;
    }

    public Map<UUID, String> getMethodNameMap() {
        return methodNameMap;
    }

    public Map<UUID, ProgramVariable> getProgramVariableMap() {
        return programVariableMap;
    }

    public Map<String, Set<UUID>> getTestRunMap() {
        return testRunMap;
    }

    public Map<UUID, DefUsePair> getDefUsePairMap() {
        return defUsePairMap;
    }
}
