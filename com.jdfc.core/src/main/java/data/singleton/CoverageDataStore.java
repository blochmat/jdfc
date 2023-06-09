package data.singleton;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.common.collect.*;
import data.*;
import data.io.CoverageDataExport;
import data.tree.ExecutionDataNode;
import graphs.cfg.LocalVariable;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.FileHelper;
import utils.JDFCUtils;
import utils.JavaParserHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * A storage singleton for all class data required for the analysis. A tree structure of {@code ExecutionDataNode}
 * instance represent the project structure of the project under test
 */
@Data
public class CoverageDataStore {
    private final Logger logger = LoggerFactory.getLogger(CoverageDataStore.class);
    private static CoverageDataStore instance;
    private final ExecutionDataNode<ExecutionData> root;
    private final List<String> testedClassList;
    private final List<String> untestedClassList;
    private String projectDirStr;
    private String buildDirStr;
    private String classesBuildDirStr;
    private List<String> srcDirStrList;

    private BiMap<UUID, ClassExecutionData> classExecutionDataBiMap;
    private BiMap<UUID, MethodData> methodDataBiMap;
    private Map<UUID, ProgramVariable> uuidProgramVariableMap;
    private Multimap<ProgramVariable, UUID> programVariableUUIDMap;
    private Map<UUID, LocalVariable> uuidLocalVariableMap;
    private Multimap<LocalVariable, UUID> localVariableUUIDMultimap;

    private CoverageDataStore() {
        ExecutionData executionData = new ExecutionData("", "");
        this.root = new ExecutionDataNode<>(executionData);
        this.testedClassList = new ArrayList<>();
        this.untestedClassList = new ArrayList<>();
        this.classExecutionDataBiMap = HashBiMap.create();
        this.methodDataBiMap = HashBiMap.create();
        this.uuidProgramVariableMap = Maps.newHashMap();
        this.programVariableUUIDMap = ArrayListMultimap.create();
        this.uuidLocalVariableMap = Maps.newHashMap();
        this.localVariableUUIDMultimap = ArrayListMultimap.create();
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
        logger.debug("saveProjectInfo");
        this.projectDirStr = projectDirStr;
        this.buildDirStr = buildDirStr;
        this.classesBuildDirStr = classesBuildDirStr;
        this.srcDirStrList = srcDirStrList;
    }

    public static void invokeCoverageTracker(final String varIdStr, final String cIdStr) {
        CoverageTracker.getInstance().addLocalVarCoveredEntry(varIdStr, cIdStr);
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

//        // Tested class data export
//        for(String className : testedClassList) {
//            logger.debug("TESTED: " + className);
//            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
//            try {
//                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
//            } catch (ParserConfigurationException | TransformerException e) {
//                e.printStackTrace();
//            }
//        }
//
//        // TODO: could be removed
//        // Untested class data export
//        for(String className : untestedClassList) {
//            logger.debug("UNTESTED: " + className);
//            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
//            try {
//                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
//            } catch (ParserConfigurationException | TransformerException e) {
//                String stackTrace = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
//                logger.debug("Exception:" + e.getMessage());
//                logger.debug(stackTrace);
//            }
//        }

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
        logger.debug("findClassDataNode");
        ArrayList<String> nodePath = new ArrayList<>(Arrays.asList(pClassName.replace(File.separator, "/").split("/")));

        // root path
        if (nodePath.size() == 1) {
            nodePath.add(0, "default");
        }

        ExecutionDataNode<ExecutionData> node = root.getChildDataRecursive(nodePath);
        if (node == null) {
            logger.debug("Return NULL");
        }
        return node;
    }

    public void addNodesFromDirRecursive(File pFile,
                                         ExecutionDataNode<ExecutionData> pExecutionDataNode,
                                         Path pBaseDir,
                                         String suffix) {
        logger.debug("addNodesFromDirRecursive");
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
                                    UUID cId = UUID.randomUUID();
                                    ClassExecutionData classNodeData = new ClassExecutionData(fqn, f.getName(), relativePath, cu, cId);
                                    untestedClassList.add(relativePath);
                                    classExecutionDataBiMap.put(cId, classNodeData);

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
    }

    private String createFqn(ExecutionDataNode<ExecutionData> node, String childName) {
        logger.debug("createFqn");
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
