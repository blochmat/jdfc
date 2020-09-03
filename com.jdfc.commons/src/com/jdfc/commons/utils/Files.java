package com.jdfc.commons.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Files {

    public static List<File> loadFilesFromDirRecursive(File file, String suffix) {
        List<File> returnList = new ArrayList<>();
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                returnList.addAll(loadFilesFromDirRecursive(child, suffix));
            }
        } else {
            if (file.getName().endsWith(suffix)){
                returnList.add(file);
            }
        }
        return returnList;
    }
}
