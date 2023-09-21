package utils;

import data.ProjectData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class Deserializer {

    public static ProjectData deserializeCoverageData(String fileAbs) {
        ProjectData obj = null;
        try {
            // Create a file input stream
            File file = new File(fileAbs);

            if(file.exists()) {
                FileInputStream fileIn = new FileInputStream(fileAbs);

                // Create an ObjectInputStream
                ObjectInputStream in = new ObjectInputStream(fileIn);

                // Read the object
                obj = (ProjectData) in.readObject();

                // Close the streams
                in.close();
                fileIn.close();

                return obj;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
