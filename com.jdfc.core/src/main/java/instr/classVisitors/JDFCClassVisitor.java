package instr.classVisitors;

import data.ClassExecutionData;
import cfg.data.LocalVariable;
import data.ProgramVariable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class JDFCClassVisitor extends ClassVisitor {

    public final ClassNode classNode;
    public final ClassExecutionData classExecutionData;
    public final Map<String, Map<Integer, LocalVariable>> localVariableTables;
    public final Set<ProgramVariable> fields;
    public final String jacocoMethodName = "$jacoco";

    public JDFCClassVisitor(final int pApi,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData) {
        super(pApi);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
        localVariableTables = new HashMap<>();
        fields = new HashSet<>();
    }

    public JDFCClassVisitor(final int pApi,
                            final ClassVisitor pClassVisitor,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData) {
        super(pApi, pClassVisitor);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
        localVariableTables = new HashMap<>();
        fields = new HashSet<>();
    }

    public JDFCClassVisitor(final int pApi,
                            final ClassNode pClassNode,
                            final ClassExecutionData pClassExecutionData,
                            final Set<ProgramVariable> fields,
                            final Map<String, Map<Integer, LocalVariable>> pLocalVariableTables) {
        super(pApi);
        classNode = pClassNode;
        classExecutionData = pClassExecutionData;
        localVariableTables = pLocalVariableTables;
        this.fields = fields;
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
        if (cv != null) {
            return cv.visitField(access, name, descriptor, signature, value);
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        final MethodVisitor mv;
        if (cv != null) {
            mv = cv.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        } else {
            mv = null;
        }

        return mv;
    }

    public MethodNode getMethodNode(final String pName,
                                    final String pDescriptor) {
        for (MethodNode node : classNode.methods) {
            if (node.name.equals(pName) && node.desc.equals(pDescriptor)) {
                return node;
            }
        }
        return null;
    }

    protected boolean isInstrumentationRequired(String pString) {
        return !pString.contains(jacocoMethodName);
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public Set<ProgramVariable> getFields() {
        return fields;
    }

    /**
     * Returns the information of the local variable tables for each method in a map of method name
     * and a respresentation of the local variable table.
     *
     * @return A map of method name to a map representing a local variable table
     */
    public Map<String, Map<Integer, LocalVariable>> getLocalVariableTables() {
        return localVariableTables;
    }

}
