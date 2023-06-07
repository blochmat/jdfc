package graphs.cfg;

import graphs.cfg.visitors.classVisitors.CFGLocalVariableClassVisitor;
import graphs.cfg.visitors.classVisitors.CFGNodeClassVisitor;
import com.google.common.base.Preconditions;
import data.ClassExecutionData;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link CFG}s for each method of a class file.
 */
public class CFGCreator {

    private static final Logger logger = LoggerFactory.getLogger(CFGCreator.class);

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
        logger.debug("createCFGsForClass");
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

        logger.debug("CFG CREATION DONE");
    }
}
