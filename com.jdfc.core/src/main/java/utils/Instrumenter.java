package utils;

import data.singleton.CoverageDataStore;
import instr.JDFCInstrument;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class Instrumenter {

    private final String workDirAbs;
    private final String classesDirAbs;

    public Instrumenter(String workDirAbs, String classesDirAbs) {
        this.workDirAbs = workDirAbs;
        this.classesDirAbs = classesDirAbs;
    }

    public void instrumentClass(String classFileAbs) {
        CoverageDataStore.getInstance().addClassData(classesDirAbs, classFileAbs);
        File classFile = new File(classFileAbs);
        String packagePath = classFile.getAbsolutePath().replace(classesDirAbs, "").replace(classFile.getName(), "");
        File outDir = new File(String.join(File.separator, workDirAbs, ".jdfc_instrumented", packagePath));
        if(!outDir.exists()) {
            outDir.mkdirs();
        }
        String outPath = String.join(File.separator, outDir.getAbsolutePath(), classFile.getName());
        try (FileOutputStream fos = new FileOutputStream(outPath)){
            byte[] classFileBuffer = Files.readAllBytes(classFile.toPath());
            ClassReader cr = new ClassReader(classFileBuffer);
            JDFCInstrument jdfcInstrument = new JDFCInstrument();
            byte[] instrumented = jdfcInstrument.instrument(cr);
            fos.write(instrumented);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] instrumentClass(byte[] classFileBuffer, String classFileAbs) {
        CoverageDataStore.getInstance().addClassData(classesDirAbs, classFileAbs);
        ClassReader cr = new ClassReader(classFileBuffer);
        JDFCInstrument jdfcInstrument = new JDFCInstrument();
        return jdfcInstrument.instrument(cr);
    }

    public List<File> loadClassFiles() {
        List<File> classFiles = new ArrayList<>();
        try {
            Files.walkFileTree(Paths.get(classesDirAbs), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".class")) {
                        classFiles.add(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classFiles;
    }
}
