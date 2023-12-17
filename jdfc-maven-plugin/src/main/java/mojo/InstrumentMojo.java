package mojo;

import data.ProjectData;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import instr.Instrumenter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;

@Mojo(name = "instrument", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class InstrumentMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Override
    public void execute()  {
        // TODO
        final String workDirAbs = project.getBasedir().toString();
        final String buildDirAbs = project.getBuild().getDirectory();
        final String classesDirAbs = project.getBuild().getOutputDirectory();
        final String sourceDirAbs = project.getBuild().getSourceDirectory();
        ProjectData.getInstance().saveProjectInfo(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
        Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs, sourceDirAbs, true);
        List<File> classFiles = instrumenter.loadClassFiles();
        // Print class path
        System.out.println(System.getProperty("java.class.path").replace(":", "\n"));
        for (File classFile : classFiles) {
            instrumenter.instrumentClass(classFile.getAbsolutePath());
        }

        try {
            Path classesDirPath = Paths.get(classesDirAbs);
            copyDirectoryAndSubdirectories(classesDirPath, Paths.get(buildDirAbs + "/jdfc-backup"));
            copyDirectoryAndSubdirectories(Paths.get(workDirAbs + "/.jdfc_instrumented"), classesDirPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyDirectoryAndSubdirectories(Path sourceDir, Path targetDir) throws IOException {
        // Check if source directory exists
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Source directory does not exist or is not a directory.");
        }

        // Create target directory if it doesn't exist
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // Copy files recursively
        Files.walkFileTree(sourceDir, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Create corresponding target directory
                Path targetPath = targetDir.resolve(sourceDir.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Copy individual file
                Path targetPath = targetDir.resolve(sourceDir.relativize(file));
                Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
