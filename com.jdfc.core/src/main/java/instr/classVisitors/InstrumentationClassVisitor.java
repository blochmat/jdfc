package instr.classVisitors;

import data.ClassExecutionData;
import instr.methodVisitors.InstrumentationMethodVisitor;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ASMHelper;

import static org.objectweb.asm.Opcodes.*;

public class InstrumentationClassVisitor extends JDFCClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(InstrumentationClassVisitor.class);

    private final ASMHelper asmHelper = new ASMHelper();

    public String className;

    private static final String TEST_DATA = "__jdfc_test_data";
    private static final String TEST_DATA_DESCRIPTOR = "Ljava/util/Map;";
    private static final String TEST_DATA_SIGNATURE = "Ljava/util/Map<Ljava/lang/String;Ljava/util/Set<Ljava/util/UUID;>;>;";

    private static final String METHOD_INIT = "__jdfc_initialize";
    private static final String METHOD_INIT_DESCRIPTOR = "()V";

    private static final String METHOD_TRACK = "__jdfc_track";
    private static final String METHOD_TRACK_DESCRIPTOR = "(Ljava/lang/String;Ljava/util/UUID;)V";

    private static final String METHOD_GET_AND_RESET = "__jdfc_getAndReset";
    private static final String METHOD_GET_AND_RESET_DESCRIPTOR = "()Ljava/util/Map;";
    private static final String METHOD_GET_AND_RESET_SIGNATURE = "()Ljava/util/Map<Ljava/lang/String;Ljava/util/Set<Ljava/util/UUID;>;>;";


    public InstrumentationClassVisitor(final ClassVisitor pClassVisitor,
                                       final ClassNode pClassNode,
                                       final ClassExecutionData pClassExecutionData) {
        super(ASM5, pClassVisitor, pClassNode, pClassExecutionData);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
        FieldVisitor fv = cv.visitField(
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_TRANSIENT,
                TEST_DATA,
                TEST_DATA_DESCRIPTOR,
                TEST_DATA_SIGNATURE,
                null);
        fv.visitEnd();
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

    private void createInitTestData() {
        MethodVisitor mvInitTestData = cv.visitMethod(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                METHOD_INIT,
                METHOD_INIT_DESCRIPTOR,
                null,
                null);
        // Start generating the method
        mvInitTestData.visitCode();
        // Create a label pointing to the end of the method
        Label labelEnd = new Label();
        // if (testData == null)
        mvInitTestData.visitFieldInsn(Opcodes.GETSTATIC, this.className, TEST_DATA, TEST_DATA_DESCRIPTOR);
        mvInitTestData.visitJumpInsn(Opcodes.IFNONNULL, labelEnd);
        // testData = new HashMap<>();
        mvInitTestData.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
        mvInitTestData.visitInsn(Opcodes.DUP);
        mvInitTestData.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mvInitTestData.visitFieldInsn(Opcodes.PUTSTATIC, this.className, TEST_DATA, TEST_DATA_DESCRIPTOR);
        // Label for end of method and return statement
        mvInitTestData.visitLabel(labelEnd);
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
        mvTrack.visitFieldInsn(Opcodes.GETSTATIC, this.className, TEST_DATA, "Ljava/util/Map;");
        mvTrack.visitVarInsn(Opcodes.ALOAD, 0);  // Load 'key' onto stack
        mvTrack.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        mvTrack.visitVarInsn(Opcodes.ASTORE, 2);
        // Start of if (testData.get(key) == null)
        mvTrack.visitVarInsn(Opcodes.ALOAD, 2); // Load the stored set
        Label notNullLabel = new Label();
        mvTrack.visitJumpInsn(Opcodes.IFNONNULL, notNullLabel);
        // if null
        mvTrack.visitTypeInsn(Opcodes.NEW, "java/util/HashSet");
        mvTrack.visitInsn(Opcodes.DUP);
        mvTrack.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashSet", "<init>", "()V", false);
        mvTrack.visitVarInsn(Opcodes.ASTORE, 2); // Store the new set into local variable 2
        // Put new set into map
        mvTrack.visitFieldInsn(Opcodes.GETSTATIC, this.className, TEST_DATA, "Ljava/util/Map;");
        mvTrack.visitVarInsn(Opcodes.ALOAD, 0); // Load the key parameter
        mvTrack.visitVarInsn(Opcodes.ALOAD, 2); // Load the new set
        mvTrack.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        mvTrack.visitInsn(Opcodes.POP); // Discard the returned old value
        // if not null
        // put element into appropriate set
        mvTrack.visitLabel(notNullLabel);
        mvTrack.visitVarInsn(Opcodes.ALOAD, 2);  // Load 'set' onto stack
        mvTrack.visitVarInsn(Opcodes.ALOAD, 1);  // Load 'value' onto stack
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
        mvGetAndReset.visitFieldInsn(GETSTATIC, this.className, TEST_DATA, "Ljava/util/Map;");
        mvGetAndReset.visitVarInsn(ASTORE, 0);
        mvGetAndReset.visitTypeInsn(NEW, "java/util/HashMap");
        mvGetAndReset.visitInsn(DUP);
        mvGetAndReset.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mvGetAndReset.visitFieldInsn(PUTSTATIC, this.className, "testData", "Ljava/util/Map;");
        // Return 'local'
        mvGetAndReset.visitVarInsn(ALOAD, 0);
        mvGetAndReset.visitInsn(ARETURN);
        // Compute max stack and max locals
        mvGetAndReset.visitMaxs(0, 0);
        mvGetAndReset.visitEnd();
    }

}