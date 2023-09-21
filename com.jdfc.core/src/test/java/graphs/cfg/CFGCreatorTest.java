package graphs.cfg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import data.ClassData;
import data.MethodData;
import data.PackageData;
import data.ProjectData;
import data.visitors.CreateClassDataVisitor;
import graphs.cfg.nodes.CFGEntryNode;
import graphs.cfg.nodes.CFGExitNode;
import graphs.cfg.visitors.classVisitors.CFGClassVisitor;
import graphs.cfg.visitors.classVisitors.LocalVariableClassVisitor;
import instr.ClassMetaData;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class CFGCreatorTest {

    private ClassReader classReader;
    private String cwd;
    private String buildDirAbs;
    private String classesDirAbs;
    private String sourcesDirAbs;

    @Before
    public void init() {
        this.cwd = System.getProperty("user.dir");
        String resources = this.cwd + "/src/test/resources";
        this.buildDirAbs = resources + "/logging";
        this.classesDirAbs = resources + "/classes";
        this.sourcesDirAbs = resources + "/sources";
        ProjectData.getInstance().saveProjectInfo(this.cwd, this.buildDirAbs, this.classesDirAbs, this.sourcesDirAbs);
    }

    @Test
    public void test_CFG_Calculator() {
        // Expected
        Multimap<Integer, Integer> expectedEdgesInit = ArrayListMultimap.create();
        expectedEdgesInit.put(0, 1);
        expectedEdgesInit.put(1, 2);
        expectedEdgesInit.put(2, 3);
        expectedEdgesInit.put(3, 4);

        Multimap<Integer, Integer> expectedEdges = ArrayListMultimap.create();
        expectedEdges.put(0, 1);
        expectedEdges.put(1, 2);
        expectedEdges.put(2, 3);
        expectedEdges.put(3, 4);
        expectedEdges.put(4, 5);

        // Arrange
        String classFileAbs = this.classesDirAbs + "/com/jdfc/Calculator.class";

        byte[] classFileBuffer;
        try {
            classFileBuffer = Files.readAllBytes(Paths.get(classFileAbs));
            this.classReader = new ClassReader(classFileBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Create ClassNode
        ClassNode classNode = new ClassNode();
        this.classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        // Create ClassMetaData
        ClassMetaData classMetaData = new ClassMetaData(classesDirAbs, sourcesDirAbs, classFileAbs);
        ProjectData.getInstance().getClassMetaDataMap().put(classMetaData.getFqn(), classMetaData);
        // Create PackageData
        String packageRel = classMetaData.getClassFilePackageRel();
        ProjectData.getInstance().getPackageDataMap().put(packageRel, new PackageData(packageRel));
        // Create ClassData and MethodData
        CreateClassDataVisitor createClassDataVisitor = new CreateClassDataVisitor(classNode, classMetaData);
        classReader.accept(createClassDataVisitor, 0);
        // Get ClassData
        UUID classDataId = ProjectData.getInstance().getClassMetaDataMap().get(classMetaData.getFqn()).getClassDataId();
        ClassData classData = ProjectData.getInstance().getClassDataMap().get(classDataId);
        // Find local variables for all methods
        LocalVariableClassVisitor localVariableVisitor = new LocalVariableClassVisitor(classNode, classData);
        classReader.accept(localVariableVisitor, 0);

        // Act
        final CFGClassVisitor cfgClassVisitor = new CFGClassVisitor(classNode, classData);
        classReader.accept(cfgClassVisitor, ClassReader.EXPAND_FRAMES);


        // Assert
        assertEquals(3, classData.getMethodDataIds().size());
        for (UUID methodId : classData.getMethodDataIds()) {
            MethodData methodData = ProjectData.getInstance().getMethodDataMap().get(methodId);
            if (methodData.getName().equals("<init>")) {
                assertEquals(5, methodData.getCfg().getNodes().size());
                assertEquals(1, methodData.getCfg().getNodes().values()
                        .stream()
                        .filter(x -> x instanceof CFGEntryNode)
                        .collect(Collectors.toSet()).size());
                assertEquals(1, methodData.getCfg().getNodes().values()
                        .stream()
                        .filter(x -> x instanceof CFGExitNode)
                        .collect(Collectors.toSet()).size());
                assertEquals(expectedEdgesInit, methodData.getCfg().getEdges());
            } else {
                assertEquals(6, methodData.getCfg().getNodes().size());
                assertEquals(1, methodData.getCfg().getNodes().values()
                        .stream()
                        .filter(x -> x instanceof CFGEntryNode)
                        .collect(Collectors.toSet()).size());
                assertEquals(1, methodData.getCfg().getNodes().values()
                        .stream()
                        .filter(x -> x instanceof CFGExitNode)
                        .collect(Collectors.toSet()).size());
                assertEquals(expectedEdges, methodData.getCfg().getEdges());
            }
        }
    }
}
