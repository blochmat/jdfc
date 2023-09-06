package utils;

import data.singleton.CoverageDataStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static utils.Constants.JDFC_SERIALIZATION_FILE;

public class Serializer {
    public static void serializeCoverageData(String workDirAbs) {
        try {
            // Create a file to save the object to
            String fileAbs = workDirAbs.replace("/", File.separator)
                    .concat(File.separator)
                    .concat(JDFC_SERIALIZATION_FILE);
            FileOutputStream fileOut = new FileOutputStream(fileAbs);

            // Create an ObjectOutputStream to write the object
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            // Write the object
            out.writeObject(CoverageDataStore.getInstance());

            // Close the streams
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
