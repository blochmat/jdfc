package data.singleton;

import data.ClassExecutionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public synchronized void addLocalVarCoveredEntry(final UUID uuid) {
//        logger.debug("addLocalVarCoveredEntry");
//        this.updateClassExecutionData(pClassName);
//        logger.debug("updateClassExecutionData");
//
//        CoverageDataStore.getInstance().getTestedClassList().add(currentClassExecutionData.getRelativePath());
//        logger.debug("getTestedClassList");
//        CoverageDataStore.getInstance().getUntestedClassList().remove(currentClassExecutionData.getRelativePath());
//        logger.debug("getUntestedClassList");
//
//        ProgramVariable pVar = mData.findVar(localPVar);
//
//        if (pVar != null && !pVar.isCovered()) {
//            pVar.setCovered(true);
//        }
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
