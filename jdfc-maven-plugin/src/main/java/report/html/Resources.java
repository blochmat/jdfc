package report.html;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Resources {

    private final File folder;

    public Resources(final File pOut) {
        String resourcesPath = String.format("%s/resources", pOut.getAbsolutePath());
        System.err.println("[DEBUG] resourcePath = " + resourcesPath);
        folder = new File(resourcesPath);
    }

    public void copyResource() throws IOException {
        copyResource("report.css");
        copyResource("script.js");
    }

    public String getPathToResourcesFrom(final File pFile) {
        Path relative = pFile.toPath().relativize(folder.toPath());
        System.err.println("[DEBUG] getPathToResourcesFrom " + relative);
        return relative.toString().replaceFirst("\\.", "");
    }

    private void copyResource(final String name) throws IOException {
        if (folder.exists() || folder.mkdir()) {
            String newFilePath = String.format("%s/%s", folder.getAbsolutePath(), name);
            File newFile = new File(newFilePath);
            final InputStream in = Resources.class.getResourceAsStream("/main/resources/" + name);
            final OutputStream out = Files.newOutputStream(newFile.toPath());
            byte[] buffer = new byte[256];
            if (in != null) {
                int len = in.read(buffer);
                while (len != -1) {
                    out.write(buffer, 0, len);
                    len = in.read(buffer);
                }
                in.close();
            }
            out.close();
        }
    }
}
