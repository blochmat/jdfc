package data;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A storage singleton for all class data required for the analysis. A tree structure of {@code ExecutionDataNode}
 * instance represent the project structure of the project under test
 */
public class CoverageDataStore {
    private final Logger logger = LoggerFactory.getLogger(CoverageDataStore.class);
    private final ExecutionDataNode<ExecutionData> root;
    private final List<String> testedClassList;
    private final List<String> untestedClassList;
    private String projectDirStr;
    private String buildDirStr;
    private String classesBuildDirStr;
    private List<String> srcDirStrList;

    private CoverageDataStore() {
        ExecutionData executionData = new ExecutionData("", "");
        this.root = new ExecutionDataNode<>(executionData);
        this.testedClassList = new ArrayList<>();
        this.untestedClassList = new ArrayList<>();

    }

    private static class Container {
        static CoverageDataStore instance = new CoverageDataStore();
    }

    public static CoverageDataStore getInstance() {
        return Container.instance;
    }

    public ExecutionDataNode<ExecutionData> getRoot() {
        return root;
    }

    public List<String> getTestedClassList() {
        return testedClassList;
    }

    public List<String> getUntestedClassList() {
        return untestedClassList;
    }

    public String getProjectDirStr() {
        return projectDirStr;
    }

    public String getBuildDirStr() {
        return buildDirStr;
    }

    public String getClassesBuildDirStr() {
        return classesBuildDirStr;
    }

    public List<String> getSrcDirStrList() {
        return srcDirStrList;
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

    public static void invokeCoverageTracker(final String pClassName,
                                             final String pInternalMethodName,
                                             final int pVarIndex,
                                             final int pInsnIndex,
                                             final int pLineNumber,
                                             final int pOpcode) {
        CoverageTracker.getInstance().addLocalVarCoveredEntry(pClassName, pInternalMethodName, pVarIndex, pInsnIndex,
                pLineNumber, pOpcode);
    }

    public void exportCoverageData() {
        logger.debug("exportCoverageData");

        // Summary export
        try {
            CoverageDataExport.dumpCoverageDataToFile();
        } catch (ParserConfigurationException | TransformerException e) {
            throw new RuntimeException(e);
        }

        // Tested class data export
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
        for(String className : untestedClassList) {
            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
            try {
                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        }
    }

    public ExecutionDataNode<ExecutionData> findClassDataNode(String pClassName) {
        logger.debug("findClassDataNode");
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
                    String relativePathWithType = pBaseDir.relativize(f.toPath()).toString();
                    String relativePath = relativePathWithType.split("\\.")[0].replace(File.separator, "/");
                    // Add className to classList of storage. Thereby we determine, if class needs to be instrumented
                    untestedClassList.add(relativePath);
                    String nameWithoutType = f.getName().split("\\.")[0];

                    // Get AST of source file
                    for(String src : CoverageDataStore.getInstance().getSrcDirStrList()) {
                        String relSourceFileStr = relativePathWithType.replace(".class", ".java");
                        String sourceFileStr = String.format("%s/%s", src, relSourceFileStr);
                        File sourceFile = new File(sourceFileStr);
                        if (sourceFile.exists()) {
                            try {
                                CompilationUnit cu = javaParserHelper.parse(sourceFile);
                                if (!isOnlyInterface(cu)) {
                                    ClassExecutionData classNodeData = new ClassExecutionData(fqn, f.getName(), relativePath, cu);
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
