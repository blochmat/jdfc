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
        getLog().info("Preparing JDFC agent for analysis. ");
        final String projectDirStr = getProject().getBasedir().toString(); // /home/path/to/project/root
        final String buildDirStr = getProject().getBuild().getDirectory(); // default: target
        final String classesBuildDirStr = getProject().getBuild().getOutputDirectory(); // default: target/classes
        final List<String> sourceDirStrList = getProject().getCompileSourceRoots(); // [/home/path/to/project/src,..]

        JDFCInstrument jdfcInstrument = new JDFCInstrument(projectDirStr, buildDirStr, classesBuildDirStr, sourceDirStrList.get(0));

        // load class files from build dir
        List<File> classFiles = new ArrayList<>();
        try {
            Files.walkFileTree(Paths.get(classesBuildDirStr), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new FileVisitor<Path>() {

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
            String packagePath = classFile.getAbsolutePath().replace(classesBuildDirStr, "").replace(classFile.getName(), "");
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
