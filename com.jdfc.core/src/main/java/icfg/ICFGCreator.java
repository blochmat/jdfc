package icfg;

import com.google.common.base.Preconditions;
import data.ClassExecutionData;
import icfg.data.LocalVariable;
import icfg.data.ProgramVariable;
import icfg.visitors.classVisitors.ICFGNodeClassVisitor;
import icfg.visitors.classVisitors.ICFGVariableClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates {@link ICFG}s for each method of a class file.
 */
public class ICFGCreator {

    private static final Logger logger = LoggerFactory.getLogger(ICFGCreator.class);

    private ICFGCreator() {
    }

    /**
     * Creates the {@link ICFG}s for each method of a class and returns a map of method name to {@link
     * ICFG}.
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
     * <p>This method is the only method to start the generation of {@link ICFG}s. The full creation
     * process of the graphs is then done internally and only the final graphs will be given back to
     * the user.
     *
     * @param pClassReader The class reader instance on the class
     * @param pClassNode   An empty class node to start the analysis with
     *
     */
    public static void createICFGsForClass(final ClassReader pClassReader,
                                           final ClassNode pClassNode,
                                           final ClassExecutionData pClassExecutionData) {
        logger.debug("createICFGsForClass");
        Preconditions.checkNotNull(pClassReader, "We need a non-null class reader to generate CFGs from.");
        Preconditions.checkNotNull(pClassNode, "We need a non-null class node to generate CFGs from.");
        Preconditions.checkNotNull(pClassExecutionData,
                "We need a non-null class execution data to generate CFGs from.");

        // Get local variable information
        final ICFGVariableClassVisitor localVariableVisitor = new ICFGVariableClassVisitor(pClassNode, pClassExecutionData);
        pClassReader.accept(localVariableVisitor, 0);

        final Set<ProgramVariable> fields = localVariableVisitor.getFields();
        final Map<String, Map<Integer, LocalVariable>> localVariableTables =
                localVariableVisitor.getLocalVariableTables();

        logger.debug(JDFCUtils.prettyPrintSet(fields));
        logger.debug(JDFCUtils.prettyPrintMap(localVariableTables));

        // Create method CFGs with entry, exit, call, return nodes
        final Map<String, ICFG> methodICFGs = new HashMap<>();
        final ICFGNodeClassVisitor ICFGNodeClassVisitor =
                new ICFGNodeClassVisitor(pClassNode, pClassExecutionData, methodICFGs, fields, localVariableTables);
        pClassReader.accept(ICFGNodeClassVisitor, 0);
        logger.debug(JDFCUtils.prettyPrintMap(methodICFGs));

        // TODO: Connect ICFGS
    }

    /**
     * Computes the internal method name representation that is used, for example, in the map emitted
     * by {@link ICFGCreator#createICFGsForClass(ClassReader, ClassNode, ClassExecutionData)}.
     *
     * @param pMethodName The name of the method
     * @param pDescriptor The method's descriptor
     * @param pSignature  The method's signature
     * @param pExceptions An array of exceptions thrown by the method
     * @return The internal method name representation
     */
    public static String computeInternalMethodName(
            final String pMethodName,
            final String pDescriptor,
            final String pSignature,
            final String[] pExceptions) {
        logger.debug("computeInternalMethodName");
        final StringBuilder result = new StringBuilder();
        result.append(pMethodName);
        result.append(": ");
        result.append(pDescriptor);
        if (pSignature != null) {
            result.append("; ").append(pSignature);
        }
        if (pExceptions != null && pExceptions.length != 0) {
            result.append("; ").append(Arrays.toString(pExceptions));
        }
        return result.toString();
    }
}
