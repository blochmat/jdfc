package utils;

import data.singleton.CoverageDataStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static utils.Constants.JDFC_SERIALIZATION_FILE;

public class Deserializer {

    public static CoverageDataStore deserializeCoverageData(String workDirAbs) {
        CoverageDataStore obj = null;
        try {
            // Create a file input stream
            String fileAbs = String.join(File.separator, workDirAbs, JDFC_SERIALIZATION_FILE);
            FileInputStream fileIn = new FileInputStream(fileAbs);

            // Create an ObjectInputStream
            ObjectInputStream in = new ObjectInputStream(fileIn);

            // Read the object
            obj = (CoverageDataStore) in.readObject();

            // Close the streams
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
