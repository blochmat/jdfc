package data;

import ifg.data.LocalVariable;
import ifg.data.ProgramVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class CoverageTracker {

    private static CoverageTracker singleton;
    private ClassExecutionData currentClassExecutionData = null;
    private final Map<String, ClassExecutionData> bla = new HashMap<>();

    public static synchronized CoverageTracker getInstance() {
        if (singleton == null) {
            singleton = new CoverageTracker();
        }
        return singleton;
    }

    public void addLocalVarCoveredEntry(final String pClassName,
                                        final String pInternalMethodName,
                                        final int pVarIndex,
                                        final int pInsnIndex,
                                        final int pLineNumber,
                                        final int pOpcode) {
        String info = String.format("%s %s %s", pClassName, pInternalMethodName, pLineNumber);
        updateClassExecutionData(pClassName);
        boolean isDefinition = isDefinition(pOpcode);
        LocalVariable localVariable = currentClassExecutionData.findLocalVariable(pInternalMethodName, pVarIndex);

        if (localVariable != null) {
            ProgramVariable programVariable =
                    ProgramVariable.create(null, localVariable.getName(), localVariable.getDescriptor(), pInsnIndex, pLineNumber, isDefinition);
            addCoveredEntry(pInternalMethodName, currentClassExecutionData, programVariable);
        }
    }

    private void updateClassExecutionData(final String pClassName) {
        if (currentClassExecutionData == null || !currentClassExecutionData.getRelativePath().equals(pClassName)) {
            if(bla.containsKey(pClassName)) {
                currentClassExecutionData = bla.get(pClassName);
            } else {
                currentClassExecutionData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
                bla.put(currentClassExecutionData.getRelativePath(), currentClassExecutionData);
            }
        }
    }

    private static void addCoveredEntry(final String methodNameDesc,
                                        final ClassExecutionData classExecutionData,
                                        final ProgramVariable programVariable) {
        if (programVariable != null) {
            Map<String, Set<ProgramVariable>> coveredList = classExecutionData.getVariablesCovered();
            coveredList.get(methodNameDesc).add(programVariable);
        }
    }

    static boolean isDefinition(final int pOpcode) {
        switch (pOpcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                return true;
            default:
                return false;
        }
    }
}
