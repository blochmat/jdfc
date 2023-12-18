package mojo;

import data.ProjectData;
import instr.Instrumenter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import report.ReportGenerator;
import utils.Deserializer;
import utils.JDFCUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "report", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class ReportMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(property = "report.createXml", defaultValue = "false")
    private String createXml;

    @Parameter(property = "report.createHtml", defaultValue = "false")
    private String createHtml;

    @Override
    public void execute() {
        // Recover ProjectData
        ProjectData deserialized = Deserializer.deserializeCoverageData(JDFCUtils.getJDFCSerFileAbs());
        if(deserialized == null) {
            throw new IllegalArgumentException("Unable do deserialize coverage data.");
        }
        ProjectData.getInstance().fetchDataFrom(deserialized);
        this.analyseUntestedClasses();
        final String outDirAbs = String.format("%s%sjdfc-report", ProjectData.getInstance().getBuildDir().getAbsolutePath(), File.separator);
        final String sourceDirAbs = project.getBuild().getSourceDirectory();
        final boolean xml = Objects.equals(createXml, "true");
        final boolean html = Objects.equals(createHtml, "true");

        ReportGenerator reportGenerator = new ReportGenerator(outDirAbs, sourceDirAbs, xml, html);
        reportGenerator.create();
    }

    private void analyseUntestedClasses() {
        String workDirAbs = ProjectData.getInstance().getWorkDir().getAbsolutePath();
        String classesDirAbs = ProjectData.getInstance().getClassesDir().getAbsolutePath();
        String sourceDirAbs = String.join(File.separator, workDirAbs, ProjectData.getInstance().getSourceDirRel());
        boolean isInterProcedural = ProjectData.getInstance().isInterProcedural();
        Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs, sourceDirAbs, isInterProcedural);
        List<String> classFileAbsList = this.getClassFileAbsListFrom(ProjectData.getInstance().getClassesDir());
        List<String> untested = classFileAbsList
                .stream()
                .filter(absPath -> ProjectData.getInstance().getClassMetaDataMap().values()
                        .stream()
                        .noneMatch(tested -> Objects.equals(tested.getClassFileAbs(), absPath)))
                .collect(Collectors.toList());
        for (String classFileAbs : untested) {
            instrumenter.instrumentClass(classFileAbs);
        }
    }

    public List<String> getClassFileAbsListFrom(File dir) {
        List<String> filePaths = new ArrayList<>();

        Path start = Paths.get(dir.getAbsolutePath()); // Replace with your directory path

        try (Stream<Path> stream = Files.walk(start)) {
            stream.map(Path::toAbsolutePath)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> filePaths.add(path.toString()));
        } catch (IOException e) {
            JDFCUtils.logThis(e.getMessage(), "ERROR");;
        }

        return filePaths;
    }
}
