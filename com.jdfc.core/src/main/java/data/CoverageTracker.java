package data;

import cfg.data.LocalVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class CoverageTracker {

    private static final Logger logger = LoggerFactory.getLogger(CoverageTracker.class);
    private static CoverageTracker singleton;
    private ClassExecutionData currentClassExecutionData = null;
    private final Map<String, ClassExecutionData> classExecutionDataMap = new HashMap<>();

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
        logger.debug("addLocalVarCoveredEntry");
        this.updateClassExecutionData(pClassName);
        LocalVariable localVariable = currentClassExecutionData.findLocalVariable(pInternalMethodName, pVarIndex);

        // add to testedClassList, remove from untestedClassList
        CoverageDataStore.getInstance().getTestedClassList().add(currentClassExecutionData.getRelativePath());
        CoverageDataStore.getInstance().getUntestedClassList().remove(currentClassExecutionData.getRelativePath());

        if (localVariable != null) {
            ProgramVariable programVariable = new ProgramVariable(null, localVariable.getName(),
                    localVariable.getDescriptor(), pInsnIndex, pLineNumber, this.isDefinition(pOpcode), false);
//            currentClassExecutionData.getMethodByInternalName(pInternalMethodName).findVar(programVariable).setCovered(true);
            // TODO: Delete
            logger.debug("HEHEHEH");
            currentClassExecutionData.getMethodByInternalName(pInternalMethodName).getCoveredVars().add(programVariable);
            logger.debug("HOHOHOH");
        }
    }

    private void updateClassExecutionData(final String pClassName) {
        logger.debug("updateClassExecutionData");
        if (currentClassExecutionData == null || !currentClassExecutionData.getRelativePath().equals(pClassName)) {
            if(classExecutionDataMap.containsKey(pClassName)) {
                currentClassExecutionData = classExecutionDataMap.get(pClassName);
            } else {
                currentClassExecutionData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
                classExecutionDataMap.put(currentClassExecutionData.getRelativePath(), currentClassExecutionData);
            }
        }
    }

    private boolean isDefinition(final int pOpcode) {
        logger.debug("isDefinition");
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
