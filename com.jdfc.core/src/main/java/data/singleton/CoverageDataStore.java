package data.singleton;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import data.*;
import data.io.CoverageDataExport;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;
import utils.JavaParserHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A storage singleton for all class data required for the analysis. A tree structure of {@code ExecutionDataNode}
 * instance represent the project structure of the project under test
 */
@Slf4j
@Data
public class CoverageDataStore implements Serializable {

    private static final long serialVersionUID = 1L;

    private static CoverageDataStore instance;
    private final transient ExecutionDataNode<ExecutionData> root;
    private final Map<UUID, ClassExecutionData> classExecutionDataMap;
    private final Set<String> testedClassList;
    private final Set<String> untestedClassList;
    private final Map<String, Map<String, ClassExecutionData>> projectData;
    private final Map<UUID, ProgramVariable> programVariableMap;
    private final Map<UUID, DefUsePair> defUsePairMap;
    private File workDir;
    private File buildDir;
    private File classesDir;
    private File jdfcDir;
    private String sourceDirAbs;
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
        this.projectData = new HashMap<>();
        this.programVariableMap = new HashMap<>();
        this.defUsePairMap = new HashMap<>();
    }

    public static CoverageDataStore getInstance() {
        if(instance == null) {
            instance = new CoverageDataStore();
        }
        return instance;
    }

    public static void setInstance(CoverageDataStore deserialized) {
        if(instance == null) {
            instance = deserialized;
        }
    }

    public void saveProjectInfo(String projectDirStr,
                                String buildDirStr,
                                String classesBuildDirStr,
                                String srcDirStr) {
        // print uncaught exception
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Exception occurred in thread: " + t.getName());
            e.printStackTrace();
        });
        this.workDir = new File(projectDirStr);
        this.buildDir = new File(buildDirStr);
        this.classesDir = new File(classesBuildDirStr);
        this.sourceDirAbs = srcDirStr;
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

    public void addClassData(String classesAbs, String classFileAbs) {
        JavaParserHelper javaParserHelper = new JavaParserHelper();
        File classFile = new File(classFileAbs);
        String classFileRel = classFileAbs.replace(classesAbs, "");
        String classFilePackage = classFileRel.replace(classFile.getName(), "").replaceAll("^/|/$", "");
        String classFileRelNoType = classFileRel.split("\\.")[0].replace(File.separator, "/");
        String sourceFileRel = classFileRel.replace(".class", ".java");
        String sourceFileAbs = CoverageDataStore.getInstance().getSourceDirAbs().concat(sourceFileRel);
        String fqn = classFileRelNoType.replace(File.separator, ".");

        File sourceFile = new File(sourceFileAbs);
        if (sourceFile.exists()) {
            try {
                CompilationUnit cu = javaParserHelper.parse(sourceFile);
                if (!isInterface(cu) && !isEnum(cu) && !isGeneric(cu)) {
                    UUID id = UUID.randomUUID();
                    untestedClassList.add(classFileRelNoType);
                    ClassExecutionData classNodeData = new ClassExecutionData(fqn, classFile.getName(), id, classFileRelNoType, cu);
                    classExecutionDataMap.put(id, classNodeData);

                    String nameWithoutType = classFile.getName().split("\\.")[0];
                    if(classFilePackage.equals("")) {
                        CoverageDataStore.getInstance().projectData.computeIfAbsent("default", k -> new HashMap<>());
                        projectData.get("default").put(nameWithoutType, classNodeData);
                    } else {
                        projectData.computeIfAbsent(classFilePackage, k -> new HashMap<>());
                        projectData.get(classFilePackage).put(nameWithoutType, classNodeData);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            if(classFileAbs.contains("$")) {
                System.out.println("Instrumentation of inner class was skipped: " + classFileAbs);
            } else {
                throw new RuntimeException("ERROR: Missing source file for " + classFileAbs);
            }
        }
    }

    public boolean isGeneric(CompilationUnit cu) {
        return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .allMatch(d -> d.isClassOrInterfaceDeclaration() && !d.getTypeParameters().isEmpty());
    }

    public boolean isInterface(CompilationUnit cu) {
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

    public boolean isEnum(CompilationUnit cu) {
        // Get a list of all types declared in the file
        List<TypeDeclaration<?>> types = cu.getTypes();
        // Check if there's exactly one type
        if (types.size() == 1) {
            TypeDeclaration<?> type = types.get(0);
            // If the single type is an interface
            return type.isEnumDeclaration() && type.asClassOrInterfaceDeclaration().isEnumDeclaration();
        }
        return false;
    }
}
