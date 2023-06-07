package data.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import data.ClassExecutionData;
import data.singleton.CoverageDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CoverageDataImport {

    private final static Logger logger = LoggerFactory.getLogger(CoverageDataImport.class);

    public static void loadExecutionData(String pClassesDir, String pJDFCDir) {
        logger.debug("loadClassExecutionData");
        File classes = new File(pClassesDir);
        Path classesPath = classes.toPath();

        File jdfc = new File(pJDFCDir);
        Path jdfcPath = jdfc.toPath();

        // Loading data node structure from target/classes
        CoverageDataStore.getInstance()
                .addNodesFromDirRecursive(classes, CoverageDataStore.getInstance().getRoot(), classesPath, ".class");

        // Load xml files from target/jdfc
        List<File> jsonFileList = loadFilesFromDirRecursive(jdfc, ".json");
        ObjectMapper objectMapper = new ObjectMapper();

        for (File file : jsonFileList) {
            String relativePathWithType = jdfcPath.relativize(file.toPath()).toString();
            String relativePath = relativePathWithType.split("\\.")[0].replace(File.separator, "/");
            try {
                ClassExecutionData classExecutionData = objectMapper.readValue(file, ClassExecutionData.class);
                CoverageDataStore.getInstance().findClassDataNode(relativePath).setData(classExecutionData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

//        CoverageDataStore.getInstance().getRoot().computeClassCoverage();
        CoverageDataStore.getInstance().getRoot().aggregateDataToRootRecursive();
        logger.debug("Loading successful.");
    }

    public static List<File> loadFilesFromDirRecursive(File file, String suffix) {
        List<File> returnList = new ArrayList<>();
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                returnList.addAll(loadFilesFromDirRecursive(child, suffix));
            }
        } else {
            if (file.getName().endsWith(suffix)) {
                returnList.add(file);
            }
        }
        return returnList;
    }
}
