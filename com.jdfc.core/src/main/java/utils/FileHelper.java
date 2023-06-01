package utils;

import java.io.File;
import java.util.Arrays;

public class FileHelper {

    public boolean filesWithSuffixPresentIn(File[] fileList, String suffix) {
        return Arrays.stream(fileList).anyMatch(x -> x.getName().contains(suffix));
    }

    public boolean isMetaInfFile(File file) {
       return file.getName().equals("META-INF");
    }

}
