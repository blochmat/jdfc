package cfg.visitors.classVisitors;

import cfg.visitors.methodVisitors.CFGLocalVariableMethodVisitor;
import data.ClassExecutionData;
import data.ProgramVariable;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ASMHelper;
import utils.JDFCUtils;

import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * A class visitor that extracts the information of the local variable table for each method of a
 * class.
 *
 * @see ClassVisitor
 */
public class CFGLocalVariableClassVisitor extends JDFCClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(CFGLocalVariableClassVisitor.class);

    private final ASMHelper asmHelper = new ASMHelper();

    private final Set<ProgramVariable> fields;

    public CFGLocalVariableClassVisitor(final ClassNode pClassNode, final ClassExecutionData pClassExecutionData) {
        super(ASM5, pClassNode, pClassExecutionData);
        fields = new HashSet<>();
    }

    /**
     * {@inheritDoc}
     *
     * Used to grab all fields of a class.
     * Value is assigned in the constructor.
     *
     */
    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
        logger.debug("visitField");
        final FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
        this.fields.add(
                ProgramVariable.create(this.getClassNode().name,
                        name, descriptor, Integer.MIN_VALUE, Integer.MIN_VALUE, false));
        logger.debug(String.format("Field: %s %s %s = %s%n", JDFCUtils.getASMAccessStr(access), descriptor, name, value));
        return fv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        logger.debug("visitMethod");
        final MethodVisitor mv = super.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        final MethodNode methodNode = this.getMethodNode(pName, pDescriptor);
        final String internalMethodName = asmHelper.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        // TODO
        if (methodNode != null && isInstrumentationRequired(pName) && !internalMethodName.contains("<init>")
                && !internalMethodName.contains("<clinit>")) {
            return new CFGLocalVariableMethodVisitor(
                    this, mv, methodNode, internalMethodName);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        logger.debug("visitEnd");
        if (cv != null) {
            cv.visitEnd();
        } else {
            super.visitEnd();
        }
        // TODO: Add fields to classExecutionData
        System.out.println(fields);
    }
}
