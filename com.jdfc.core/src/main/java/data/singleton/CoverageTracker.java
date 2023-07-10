package data.singleton;

import data.ClassExecutionData;
import data.MethodData;
import data.ProgramVariable;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

@Slf4j
public class CoverageTracker {

    private static CoverageTracker singleton;

    public static synchronized CoverageTracker getInstance() {
        if (singleton == null) {
            singleton = new CoverageTracker();
        }
        return singleton;
    }

    public synchronized void addVarCoveredEntry(final String cId,
                                                final String mId,
                                                final String pId) {
        ClassExecutionData cData = null;
        MethodData mData = null;
        ProgramVariable pVar = null;
        try {
            cData = CoverageDataStore.getInstance().getClassExecutionDataMap().get(UUID.fromString(cId));
            CoverageDataStore.getInstance().getTestedClassList().add(cData.getRelativePath());
            CoverageDataStore.getInstance().getUntestedClassList().remove(cData.getRelativePath());
            mData = cData.getMethods().get(UUID.fromString(mId));
            pVar = mData.getProgramVariables().get(UUID.fromString(pId));
            if (!pVar.getIsCovered()) {
                pVar.setIsCovered(true);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                File file = JDFCUtils.createFileInDebugDir("ERROR_addLocalVarCoveredEntry.txt", false);
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write(String.format("Exception: %s ", e.getClass()));
                    writer.write(String.format("Message: %s ", e.getMessage()));
                    if (cData == null) {
                        writer.write(String.format("    cId: %s\n", cId));
                        writer.write("==============================\n");
                        writer.write("ClassExecutionDataMap:\n");
                        writer.write(JDFCUtils.prettyPrintMap(CoverageDataStore.getInstance().getClassExecutionDataMap()));
                        writer.write("==============================\n");
                    } else if (mData == null){
                        writer.write(String.format("    cData: %s\n", cData));
                        writer.write(String.format("    mId: %s\n", mId));
                        writer.write("==============================\n");
                        writer.write("Methods:\n");
                        writer.write(JDFCUtils.prettyPrintMap(cData.getMethods()));
                        writer.write("==============================\n");
                    } else if (pVar == null){
                        writer.write(String.format("    cData: %s\n", cData));
                        writer.write(String.format("    mData: %s\n", mData));
                        writer.write(String.format("    pId: %s\n", pId));
                        writer.write("==============================\n");
                        writer.write("ClassExecutionDataMap:\n");
                        writer.write(JDFCUtils.prettyPrintMap(CoverageDataStore.getInstance().getClassExecutionDataMap()));
                        writer.write("==============================\n");
                    } else {
                        writer.write(String.format("    cData.getRelativePath: %s", cData.getRelativePath()));
                        writer.write("==============================\n");
                        writer.write("UntestedClassList:\n");
                        writer.write(JDFCUtils.prettyPrintArray(CoverageDataStore.getInstance().getUntestedClassList().toArray(new String[0])));
                        writer.write("TestedClassList:\n");
                        writer.write(JDFCUtils.prettyPrintArray(CoverageDataStore.getInstance().getTestedClassList().toArray(new String[0])));
                        writer.write("==============================\n");
                    }
                    writer.write("\n");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    public synchronized void trackNewObjectRef(final String cId, final String mId, final Object obj) {
        ClassExecutionData cData = null;
        MethodData mData = null;
        try {
            cData = CoverageDataStore.getInstance().getClassExecutionDataMap().get(UUID.fromString(cId));
            CoverageDataStore.getInstance().getTestedClassList().add(cData.getRelativePath());
            CoverageDataStore.getInstance().getUntestedClassList().remove(cData.getRelativePath());
            mData = cData.getMethods().get(UUID.fromString(mId));
            mData.getAllocatedObjects().put(System.identityHashCode(obj), obj);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                File file = JDFCUtils.createFileInDebugDir("ERROR_addLocalVarCoveredEntry.txt", false);
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write(String.format("Exception: %s ", e.getClass()));
                    writer.write(String.format("Message: %s ", e.getMessage()));
                    if (cData == null) {
                        writer.write(String.format("    cId: %s\n", cId));
                        writer.write("==============================\n");
                        writer.write("ClassExecutionDataMap:\n");
                        writer.write(JDFCUtils.prettyPrintMap(CoverageDataStore.getInstance().getClassExecutionDataMap()));
                        writer.write("==============================\n");
                    } else if (mData == null){
                        writer.write(String.format("    cData: %s\n", cData));
                        writer.write(String.format("    mId: %s\n", mId));
                        writer.write("==============================\n");
                        writer.write("Methods:\n");
                        writer.write(JDFCUtils.prettyPrintMap(cData.getMethods()));
                        writer.write("==============================\n");
                    }  else {
                        writer.write(String.format("    cData.getRelativePath: %s", cData.getRelativePath()));
                        writer.write("==============================\n");
                        writer.write("UntestedClassList:\n");
                        writer.write(JDFCUtils.prettyPrintArray(CoverageDataStore.getInstance().getUntestedClassList().toArray(new String[0])));
                        writer.write("TestedClassList:\n");
                        writer.write(JDFCUtils.prettyPrintArray(CoverageDataStore.getInstance().getTestedClassList().toArray(new String[0])));
                        writer.write("==============================\n");
                    }
                    writer.write("\n");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    public synchronized void trackModifiedObjectRef(final String cId, final String mId, final Object obj) {
        ClassExecutionData cData = null;
        MethodData mData = null;
        try {
            cData = CoverageDataStore.getInstance().getClassExecutionDataMap().get(UUID.fromString(cId));
            CoverageDataStore.getInstance().getTestedClassList().add(cData.getRelativePath());
            CoverageDataStore.getInstance().getUntestedClassList().remove(cData.getRelativePath());
            mData = cData.getMethods().get(UUID.fromString(mId));
            mData.getModifiedObjects().put(System.identityHashCode(obj), obj);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                File file = JDFCUtils.createFileInDebugDir("ERROR_addLocalVarCoveredEntry.txt", false);
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write(String.format("Exception: %s ", e.getClass()));
                    writer.write(String.format("Message: %s ", e.getMessage()));
                    if (cData == null) {
                        writer.write(String.format("    cId: %s\n", cId));
                        writer.write("==============================\n");
                        writer.write("ClassExecutionDataMap:\n");
                        writer.write(JDFCUtils.prettyPrintMap(CoverageDataStore.getInstance().getClassExecutionDataMap()));
                        writer.write("==============================\n");
                    } else if (mData == null){
                        writer.write(String.format("    cData: %s\n", cData));
                        writer.write(String.format("    mId: %s\n", mId));
                        writer.write("==============================\n");
                        writer.write("Methods:\n");
                        writer.write(JDFCUtils.prettyPrintMap(cData.getMethods()));
                        writer.write("==============================\n");
                    }  else {
                        writer.write(String.format("    cData.getRelativePath: %s", cData.getRelativePath()));
                        writer.write("==============================\n");
                        writer.write("UntestedClassList:\n");
                        writer.write(JDFCUtils.prettyPrintArray(CoverageDataStore.getInstance().getUntestedClassList().toArray(new String[0])));
                        writer.write("TestedClassList:\n");
                        writer.write(JDFCUtils.prettyPrintArray(CoverageDataStore.getInstance().getTestedClassList().toArray(new String[0])));
                        writer.write("==============================\n");
                    }
                    writer.write("\n");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}