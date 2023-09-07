package report.html;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Resources {

    private final File folder;

    public Resources(final File outputDir) {
        String resourcesPath = String.format("%s/resources", outputDir.getAbsolutePath());
        folder = new File(resourcesPath);
    }

    public void copyResource() throws IOException {
        copyResource("report.css");
        copyResource("script.js");
    }

    public String getPathToResourcesFrom(final File pFile) {
        Path relative = pFile.toPath().relativize(folder.toPath());
        return relative.toString().replaceFirst("\\.", "");
    }

    private void copyResource(final String fileName) throws IOException {
        if (folder.exists() || folder.mkdir()) {
            String newFilePath = String.format("%s/%s", folder.getAbsolutePath(), fileName);
            File newFile = new File(newFilePath);
            ClassLoader classLoader = Resources.class.getClassLoader();
            try (InputStream in = classLoader.getResourceAsStream(fileName)) {
                if (in == null) {
                    throw new IllegalArgumentException("File not found");
                }
                final OutputStream out = Files.newOutputStream(newFile.toPath());
                byte[] buffer = new byte[256];
                int len = in.read(buffer);
                while (len != -1) {
                    out.write(buffer, 0, len);
                    len = in.read(buffer);
                }
                in.close();
                out.close();
            }
        }
    }
}
