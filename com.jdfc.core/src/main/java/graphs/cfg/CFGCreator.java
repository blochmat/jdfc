package graphs.cfg;

import com.google.common.base.Preconditions;
import data.ClassExecutionData;
import data.MethodData;
import graphs.cfg.visitors.classVisitors.CFGLocalVariableClassVisitor;
import graphs.cfg.visitors.classVisitors.CFGNodeClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Creates {@link CFG}s for each method of a class file.
 */
@Slf4j
public class CFGCreator {

    private CFGCreator() {
    }

    /**
     * Creates the {@link CFG}s for each method of a class and returns a map of method name to {@link
     * CFG}.
     *
     * <p>The key of the map is in the format
     *
     * <pre>
     *   name: descriptor[; signature][; exceptions]
     * </pre>
     * <p>
     * Hence, a default constructor will have the key <code>&lt;init&gt;: ()V</code> and a method
     * "foo" that takes an int and a String and returns a double array will have the key <code> foo:
     * (ILjava/lang/String;)[D</code>.
     *
     * <p>This method is the only method to start the generation of {@link CFG}s. The full creation
     * process of the graphs is then done internally and only the final graphs will be given back to
     * the user.
     *
     * @param pClassReader The class reader instance on the class
     * @param pClassNode   An empty class node to start the analysis with
     *
     */
    public static void createCFGsForClass(final ClassReader pClassReader,
                                          final ClassNode pClassNode,
                                          final ClassExecutionData pClassExecutionData) {
        Preconditions.checkNotNull(pClassReader, "We need a non-null class reader to generate CFGs from.");
        Preconditions.checkNotNull(pClassNode, "We need a non-null class node to generate CFGs from.");
        Preconditions.checkNotNull(pClassExecutionData,
                "We need a non-null class execution data to generate CFGs from.");

        // Get local variable information for all methods in the class
        final CFGLocalVariableClassVisitor localVariableVisitor =
                new CFGLocalVariableClassVisitor(pClassNode, pClassExecutionData);
        pClassReader.accept(localVariableVisitor, 0);

        // Build CFG for all methods in the class
        final CFGNodeClassVisitor CFGNodeClassVisitor =
                new CFGNodeClassVisitor(pClassNode, pClassExecutionData);
        pClassReader.accept(CFGNodeClassVisitor, 0);

        if(log.isDebugEnabled()) {
            // Log all relative paths of files in the classpath
            File transformFile = JDFCUtils.createFileInDebugDir("3_createCFGsForClass.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                for(MethodData mData : pClassExecutionData.getMethods().values()) {
                    writer.write("Method: " + mData.buildInternalMethodName());
                    writer.write(JDFCUtils.prettyPrintMap(mData.getLocalVariableTable()));
                    writer.write(JDFCUtils.prettyPrintMap(mData.getCfg().getNodes()));
                    writer.write(JDFCUtils.prettyPrintMultimap(mData.getCfg().getEdges()));
                    writer.write("\n");
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
