package report;

import data.*;
import data.ProjectData;

import java.util.UUID;
import java.util.stream.Collectors;

public class ReportGenerator {

    private final XMLReportGenerator xmlReportGenerator;
    private final HTMLReportGenerator htmlReportGenerator;

    public ReportGenerator(String outDirAbs, String sourceDirAbs) {
        this.xmlReportGenerator = new XMLReportGenerator(outDirAbs);
        this.htmlReportGenerator = new HTMLReportGenerator(outDirAbs, sourceDirAbs);
    }

    public void create() {
        this.computeProjectCoverage();
        this.xmlReportGenerator.create();
        this.htmlReportGenerator.create();
    }

    private void computeProjectCoverage() {
        this.computeVarCoverage();
        this.computePairCoverage();
        this.computeMethodCoverage();
        this.computeClassCoverage();
        this.computePackageCoverage();

        int methodCount = ProjectData.getInstance().getPackageDataMap().values().stream().map(PackageData::getMethodCount).reduce(0, Integer::sum);
        int total = ProjectData.getInstance().getPackageDataMap().values().stream().map(PackageData::getTotal).reduce(0, Integer::sum);
        int covered = ProjectData.getInstance().getPackageDataMap().values().stream().map(PackageData::getCovered).reduce(0, Integer::sum);
        double ratio = 0;
        if(total != 0) {
            ratio = (double) covered / total;
        }

        ProjectData.getInstance().setMethodCount(methodCount);
        ProjectData.getInstance().setTotal(total);
        ProjectData.getInstance().setCovered(covered);
        ProjectData.getInstance().setRatio(ratio);
    }

    private void computeVarCoverage() {
        for (String id : ProjectData.getInstance().getCoveredPVarIds()) {
            ProjectData.getInstance().getProgramVariableMap().get(UUID.fromString(id)).setIsCovered(true);
        }
    }

    private void computePairCoverage() {
        for (DefUsePair pair : ProjectData.getInstance().getDefUsePairMap().values()) {
            boolean defIsCovered = ProjectData.getInstance().getProgramVariableMap().get(pair.getDefId()).getIsCovered();
            boolean useIsCovered = ProjectData.getInstance().getProgramVariableMap().get(pair.getUseId()).getIsCovered();
            pair.setCovered(defIsCovered && useIsCovered);
        }
    }

    private void computeMethodCoverage() {
        for (MethodData methodData : ProjectData.getInstance().getMethodDataMap().values()) {
            int total = methodData.getDUPairsFromStore().size();
            int covered = methodData.getDUPairsFromStore().values().stream()
                    .filter(DefUsePair::isCovered)
                    .collect(Collectors.toSet()).size();
            double ratio = 0;
            if(total != 0) {
                ratio = (double) covered / total;
            }
            methodData.setTotal(total);
            methodData.setCovered(covered);
            methodData.setRatio(ratio);
        }
    }

    private void computeClassCoverage() {
        for (ClassData classData : ProjectData.getInstance().getClassDataMap().values()) {
            int methodCount = classData.getMethodDataFromStore().size();
            int total = classData.getMethodDataFromStore().values().stream().map(MethodData::getTotal).reduce(0, Integer::sum);
            int covered = classData.getMethodDataFromStore().values().stream().map(MethodData::getCovered).reduce(0, Integer::sum);
            double ratio = 0;
            if(total != 0) {
                ratio = (double) covered / total;
            }

            classData.setMethodCount(methodCount);
            classData.setTotal(total);
            classData.setCovered(covered);
            classData.setRatio(ratio);
        }
    }

    private void computePackageCoverage() {
        for (PackageData packageData : ProjectData.getInstance().getPackageDataMap().values()) {
            int methodCount = packageData.getClassDataFromStore().values().stream().map(ClassData::getMethodCount).reduce(0, Integer::sum);
            int total = packageData.getClassDataFromStore().values().stream().map(ClassData::getTotal).reduce(0, Integer::sum);
            int covered = packageData.getClassDataFromStore().values().stream().map(ClassData::getCovered).reduce(0, Integer::sum);
            double ratio = 0;
            if(total != 0) {
                ratio = (double) covered / total;
            }

            packageData.setMethodCount(methodCount);
            packageData.setTotal(total);
            packageData.setCovered(covered);
            packageData.setRatio(ratio);
        }
    }
}
