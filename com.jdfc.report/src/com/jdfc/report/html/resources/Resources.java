package com.jdfc.report.html.resources;

import java.io.*;
import java.nio.file.Path;

public class Resources {

    private final File folder;

    public Resources(final File pOut) {
        String resourcesPath = String.format("%s/resources", pOut.getAbsolutePath());
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

    private void copyResource(final String name) throws IOException {
        if (folder.exists() || folder.mkdir()) {
            String newFilePath = String.format("%s/%s", folder.getAbsolutePath(), name);
            File newFile = new File(newFilePath);
            final InputStream in = Resources.class.getResourceAsStream(name);
            final OutputStream out = new FileOutputStream(newFile);
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
