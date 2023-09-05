package instr.classVisitors;

import data.ClassExecutionData;
import instr.methodVisitors.InstrumentationMethodVisitor;
import lombok.Getter;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ASMHelper;

import static org.objectweb.asm.Opcodes.*;
import static utils.Constants.*;

@Getter
public class InstrumentationClassVisitor extends JDFCClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(InstrumentationClassVisitor.class);

    private final ASMHelper asmHelper = new ASMHelper();

    private String className;


    public InstrumentationClassVisitor(final ClassVisitor pClassVisitor,
                                       final ClassNode pClassNode,
                                       final ClassExecutionData pClassExecutionData) {
        super(ASM5, pClassVisitor, pClassNode, pClassExecutionData);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
        createTestData();
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        MethodVisitor mv = super.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        MethodNode methodNode = getMethodNode(pName, pDescriptor);
        final String internalMethodName = asmHelper.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        if (methodNode != null && isInstrumentationRequired(methodNode, internalMethodName) ) {
            mv = new InstrumentationMethodVisitor(this, mv, methodNode, internalMethodName);
        }

        return mv;
    }

    @Override
    public void visitEnd() {
        createInitTestData();
        createTrackVar();
        createGetAndReset();

        super.visitEnd();
    }

    private void createTestData() {
        FieldVisitor fv = cv.visitField(
                ACC_PUBLIC | ACC_STATIC,
                FIELD_TEST_DATA,
                FIELD_TEST_DATA_DESCRIPTOR,
                FIELD_TEST_DATA_SIGNATURE,
                null);
        fv.visitEnd();
    }

    private void createInitTestData() {
        MethodVisitor mvInitTestData = cv.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                METHOD_INIT,
                METHOD_INIT_DESCRIPTOR,
                null,
                null);
        // Start generating the method
        mvInitTestData.visitCode();
        // Create a label pointing to the end of the method
        Label l0 = new Label();
        mvInitTestData.visitLabel(l0);
        // if (testData == null)
        mvInitTestData.visitFieldInsn(Opcodes.GETSTATIC, this.className, FIELD_TEST_DATA, FIELD_TEST_DATA_DESCRIPTOR);
        Label l1 = new Label();
        mvInitTestData.visitJumpInsn(Opcodes.IFNONNULL, l1);
        // testData = new HashMap<>();
        Label l2 = new Label();
        mvInitTestData.visitLabel(l2);
        mvInitTestData.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/concurrent/ConcurrentHashMap", "newKeySet", "()Ljava/util/Set;", false);
        mvInitTestData.visitFieldInsn(Opcodes.PUTSTATIC, this.className, FIELD_TEST_DATA, FIELD_TEST_DATA_DESCRIPTOR);
        // Label for end of method and return statement
        mvInitTestData.visitLabel(l1);
        mvInitTestData.visitInsn(Opcodes.RETURN);
        // Complete the generation of the method
        mvInitTestData.visitMaxs(0, 0); // Computed automatically due to ClassWriter.COMPUTE_FRAMES
        mvInitTestData.visitEnd();
    }

    private void createTrackVar() {
        MethodVisitor mvTrack = cv.visitMethod(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                METHOD_TRACK,
                METHOD_TRACK_DESCRIPTOR,
                null,
                null
        );
        mvTrack.visitCode();
        // Load 'this' and 'key' and invoke map.get(key)
        mvTrack.visitFieldInsn(Opcodes.GETSTATIC, this.className, FIELD_TEST_DATA, FIELD_TEST_DATA_DESCRIPTOR);
        mvTrack.visitVarInsn(Opcodes.ALOAD, 0);  // Load 'key' onto stack
        mvTrack.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z", true);
        mvTrack.visitInsn(Opcodes.POP);  // Pop the boolean returned by Set.add()
        // Return
        mvTrack.visitInsn(Opcodes.RETURN);
        // Compute max stack and max locals
        mvTrack.visitMaxs(0, 0);
        mvTrack.visitEnd();
    }

    private void createGetAndReset() {
        MethodVisitor mvGetAndReset = cv.visitMethod(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                METHOD_GET_AND_RESET,
                METHOD_GET_AND_RESET_DESCRIPTOR,
                METHOD_GET_AND_RESET_SIGNATURE,
                null);
        mvGetAndReset.visitCode();
        // Initialize local variable 'local' by copying 'testData' to it
        mvGetAndReset.visitFieldInsn(GETSTATIC, this.className, FIELD_TEST_DATA, FIELD_TEST_DATA_DESCRIPTOR);
        mvGetAndReset.visitVarInsn(ASTORE, 0);
        mvGetAndReset.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/concurrent/ConcurrentHashMap", "newKeySet", "()Ljava/util/Set;", false);
        mvGetAndReset.visitFieldInsn(PUTSTATIC, this.className, FIELD_TEST_DATA, FIELD_TEST_DATA_DESCRIPTOR);
        // Return 'local'
        mvGetAndReset.visitVarInsn(ALOAD, 0);
        mvGetAndReset.visitInsn(ARETURN);
        // Compute max stack and max locals
        mvGetAndReset.visitMaxs(0, 0);
        mvGetAndReset.visitEnd();
    }

}