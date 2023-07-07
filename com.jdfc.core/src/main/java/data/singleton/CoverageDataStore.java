package data.singleton;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import data.*;
import data.io.CoverageDataExport;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.FileHelper;
import utils.JDFCUtils;
import utils.JavaParserHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A storage singleton for all class data required for the analysis. A tree structure of {@code ExecutionDataNode}
 * instance represent the project structure of the project under test
 */
@Slf4j
@Data
public class CoverageDataStore {
    private static CoverageDataStore instance;
    private final ExecutionDataNode<ExecutionData> root;
    private final Map<UUID, ClassExecutionData> classExecutionDataMap;
    private final Set<String> testedClassList;
    private final Set<String> untestedClassList;
    private File projectDir;
    private File buildDir;
    private File classesBuildDir;
    private File jdfcDir;
    private List<String> srcDirStrList;
    private File jdfcDebugDir;
    private File jdfcDebugInstrDir;
    private File jdfcDebugErrorDir;
    private File jdfcDebugDevLogDir;

    private CoverageDataStore() {
        ExecutionData executionData = new ExecutionData("", "");
        this.root = new ExecutionDataNode<>(executionData);
        this.classExecutionDataMap = new HashMap<>();
        this.testedClassList = new HashSet<>();
        this.untestedClassList = new HashSet<>();
    }

    public static CoverageDataStore getInstance() {
        if(instance == null) {
            instance = new CoverageDataStore();
        }
        return instance;
    }

    // Just for testing
    public static void setInstance(CoverageDataStore mock) {
        instance = mock;
    }

    public void saveProjectInfo(String projectDirStr,
                                String buildDirStr,
                                String classesBuildDirStr,
                                List<String> srcDirStrList) {
        this.projectDir = new File(projectDirStr);
        this.buildDir = new File(buildDirStr);
        this.classesBuildDir = new File(classesBuildDirStr);
        this.srcDirStrList = srcDirStrList;
        this.jdfcDir = new File(String.format("%s%sjdfc", this.buildDir, File.separator));
        this.jdfcDebugDir = new File(String.format("%s%sdebug", this.jdfcDir, File.separator));
        this.jdfcDebugInstrDir = new File(String.format("%s%sinstrumentation", this.jdfcDebugDir, File.separator));
        this.jdfcDebugErrorDir = new File(String.format("%s%serror", this.jdfcDebugDir, File.separator));
        this.jdfcDebugDevLogDir = new File(String.format("%s%slog", this.jdfcDebugDir, File.separator));
    }

    public static void trackVar(final String cId,
                                final String mId,
                                final String pId) {
        CoverageTracker.getInstance().addVarCoveredEntry(cId, mId, pId);
    }

    public static void trackNewObject(final Object obj,
                                      final String cId,
                                      final String mId) {
        CoverageTracker.getInstance().trackNewObjectRef(cId, mId, obj);
    }

    public static void trackModifiedObject(final Object obj,
                                           final String cId,
                                           final String mId) {
        CoverageTracker.getInstance().trackModifiedObjectRef(cId, mId, obj);
    }

    public void exportCoverageData() {
        Logger logger = LoggerFactory.getLogger("Global");
        long start = System.currentTimeMillis();
        logger.info("Coverage data export started.");


        // Summary export
        try {
            CoverageDataExport.dumpCoverageDataToFile();
        } catch (ParserConfigurationException | TransformerException e) {
            throw new RuntimeException(e);
        }

        // Tested class data export
        JDFCUtils.logThis(testedClassList.toString(), "tested_classList");
        for(String className : testedClassList) {
            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
            try {
                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        }

        // TODO: could be removed
        // Untested class data export
        JDFCUtils.logThis(untestedClassList.toString(), "untested_classList");
        for(String className : untestedClassList) {
            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
            try {
                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
            } catch (ParserConfigurationException | TransformerException e) {
                String stackTrace = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
                logger.debug("Exception:" + e.getMessage());
                logger.debug(stackTrace);
            }
        }

        long end = System.currentTimeMillis();
        Duration duration = Duration.ofMillis(end - start);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);
        long seconds = duration.getSeconds();
        duration = duration.minusSeconds(seconds);
        long millis = duration.toMillis();

        String time = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        logger.info(String.format("Coverage data export finished. Time: %s", time));
    }

    public ExecutionDataNode<ExecutionData> findClassDataNode(String pClassName) {
        ArrayList<String> nodePath = new ArrayList<>(Arrays.asList(pClassName.replace(File.separator, "/").split("/")));

        // root path
        if (nodePath.size() == 1) {
            nodePath.add(0, "default");
        }

        return root.getChildDataRecursive(nodePath);
    }

    public void addNodesFromDirRecursive(File pFile,
                                         ExecutionDataNode<ExecutionData> pExecutionDataNode,
                                         Path pBaseDir,
                                         String suffix) {
        JavaParserHelper javaParserHelper = new JavaParserHelper();
        FileHelper fileHelper = new FileHelper();

        File[] fileList = Objects.requireNonNull(pFile.listFiles());
        boolean containsClasses = fileHelper.filesWithSuffixPresentIn(fileList, suffix);
        if (pExecutionDataNode.isRoot() && containsClasses) {
            pExecutionDataNode.addChild("default", this.root.getData());
        }
        for (File f : fileList) {
            String fqn = createFqn(pExecutionDataNode, f.getName());
            if (f.isDirectory() && !fileHelper.isMetaInfFile(f)) {
                ExecutionData pkgData = new ExecutionData(fqn, f.getName());
                ExecutionDataNode<ExecutionData> newPkgExecutionDataNode = new ExecutionDataNode<>(pkgData);
                pExecutionDataNode.addChild(f.getName(), newPkgExecutionDataNode);
                addNodesFromDirRecursive(f, newPkgExecutionDataNode, pBaseDir, suffix);
            } else if (f.isFile() && f.getName().endsWith(suffix)) {
                // Do not handle anonymous inner files
                if(!JDFCUtils.isNestedClass(f.getName()) && !JDFCUtils.isAnonymousInnerClass(f.getName())) {
                    // Get AST of source file
                    for(String src : CoverageDataStore.getInstance().getSrcDirStrList()) {
                        String relativePathWithType = pBaseDir.relativize(f.toPath()).toString();
                        String relativePath = relativePathWithType.split("\\.")[0].replace(File.separator, "/");
                        String relSourceFileStr = relativePathWithType.replace(".class", ".java");
                        String sourceFileStr = String.format("%s/%s", src, relSourceFileStr);
                        File sourceFile = new File(sourceFileStr);
                        if (sourceFile.exists()) {
                            try {
                                CompilationUnit cu = javaParserHelper.parse(sourceFile);
                                if (!isOnlyInterface(cu)) {
                                    UUID id = UUID.randomUUID();
                                    untestedClassList.add(relativePath);
                                    ClassExecutionData classNodeData = new ClassExecutionData(fqn, f.getName(), id, relativePath, cu);
                                    classExecutionDataMap.put(id, classNodeData);

                                    String nameWithoutType = f.getName().split("\\.")[0];
                                    if (pExecutionDataNode.isRoot()) {
                                        pExecutionDataNode.getChildren().get("default").addChild(nameWithoutType, classNodeData);
                                    } else {
                                        pExecutionDataNode.addChild(nameWithoutType, classNodeData);
                                    }
                                }
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

        if (log.isDebugEnabled()) {
            File file = JDFCUtils.createFileInDebugDir("1_addNodesFromDirRecursive.txt", false);
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write("ExecutionData\n");
                writer.write("Name: ");
                writer.write(pExecutionDataNode.getData().getName());
                writer.write("\n");
                writer.write("Fqn: ");
                writer.write(pExecutionDataNode.getData().getFqn());
                writer.write("\n");
                writer.write("Parent Fqn: ");
                writer.write(pExecutionDataNode.getData().getParentFqn());
                writer.write("\n");
                writer.write("Children: ");
                writer.write(JDFCUtils.prettyPrintArray(pExecutionDataNode.getChildren().keySet().toArray()));
                writer.write("\n");
                writer.write("=========================================");
                writer.write("\n");
                writer.write("CoverageDataStorage.untestedClasses");
                writer.write(JDFCUtils.prettyPrintArray(untestedClassList.toArray(new String[0])));
                writer.write("-----------------------------------------");
                writer.write("\n");
                writer.write("-----------------------------------------");
                writer.write("\n");
                writer.write("CoverageDataStorage.classExecutionDataMap: ");
                writer.write(JDFCUtils.prettyPrintMap(classExecutionDataMap));
                writer.write("=========================================");
                writer.write("\n");
                writer.write("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private String createFqn(ExecutionDataNode<ExecutionData> node, String childName) {
        if (node.isRoot()) {
            return childName;
        } else {
            return String.format("%s.%s", node.getData().getFqn(), childName);
        }
    }

    public boolean isOnlyInterface(CompilationUnit cu) {
        // Get a list of all types declared in the file
        List<TypeDeclaration<?>> types = cu.getTypes();
        // Check if there's exactly one type
        if (types.size() == 1) {
            TypeDeclaration<?> type = types.get(0);
            // If the single type is an interface
            return type.isClassOrInterfaceDeclaration() && type.asClassOrInterfaceDeclaration().isInterface();
        }
        return false;
    }
}
