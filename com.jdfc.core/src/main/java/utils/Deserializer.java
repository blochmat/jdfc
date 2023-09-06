package utils;

import data.singleton.CoverageDataStore;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static utils.Constants.JDFC_SERIALIZATION_FILE;

public class Deserializer {

    public static void deserializeCoverageData() {
        try {
            // Create a file input stream
            FileInputStream fileIn = new FileInputStream(JDFC_SERIALIZATION_FILE);

            // Create an ObjectInputStream
            ObjectInputStream in = new ObjectInputStream(fileIn);

            // Read the object
            CoverageDataStore.setInstance((CoverageDataStore) in.readObject());

            // Close the streams
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
