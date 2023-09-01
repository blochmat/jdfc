package mojo;

import instr.JDFCInstrument;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AgentMojo extends AbstractJdfcMojo {

    private static final String JDFC_INSTRUMENTED = ".jdfc_instrumented";

    /**
     * When executing the mojo we add the agent argument to the command line.
     * If ths seems unnecessarily complicated to you then you might be right.
     *
     */
    @Override
    protected void executeMojo()  {
        final String workDirAbs = getProject().getBasedir().toString();
        final String buildDirAbs = getProject().getBuild().getDirectory();
        final String classesDirAbs = getProject().getBuild().getOutputDirectory();
        final String sourceDirAbs = getProject().getCompileSourceRoots().get(0);

        System.out.println(workDirAbs);
        System.out.println(buildDirAbs);
        System.out.println(classesDirAbs);
        System.out.println(sourceDirAbs);

        JDFCInstrument jdfcInstrument = new JDFCInstrument(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);

        // load class files from build dir
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

        // Instrumenting and saving classes
        for (File classFile : classFiles) {
            String packagePath = classFile.getAbsolutePath().replace(classesDirAbs, "").replace(classFile.getName(), "");
            File out = new File(String.format("%s%s%s", JDFC_INSTRUMENTED, File.separator, packagePath));
            if(!out.exists()) {
                out.mkdirs();
            }

            String outFilePath = String.format("%s%s%s", out.getAbsolutePath(), File.separator, classFile.getName());
            try (FileOutputStream fos = new FileOutputStream(outFilePath)){
                byte[] classFileBuffer = Files.readAllBytes(classFile.toPath());
                ClassReader cr = new ClassReader(classFileBuffer);
                byte[] instrumented = jdfcInstrument.instrument(cr);
                fos.write(instrumented);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
