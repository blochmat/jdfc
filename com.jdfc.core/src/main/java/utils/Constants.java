package utils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.objectweb.asm.Opcodes.*;

public class Constants {
    public static final String FIELD_TEST_DATA = "__jdfc_test_data";
    public static final String FIELD_TEST_DATA_DESCRIPTOR = "Ljava/util/Map;";
    public static final String FIELD_TEST_DATA_SIGNATURE = "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;";

    public static final String METHOD_INIT = "__jdfc_initialize";
    public static final String METHOD_INIT_DESCRIPTOR = "()V";

    public static final String METHOD_TRACK = "__jdfc_track";
    public static final String METHOD_TRACK_DESCRIPTOR = "(Ljava/lang/String;)V";

    public static final String METHOD_GET_AND_RESET = "__jdfc_getAndReset";
    public static final String METHOD_GET_AND_RESET_DESCRIPTOR = "()Ljava/util/Map;";
    public static final String METHOD_GET_AND_RESET_SIGNATURE = "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;";

    public static final String JDFC_DIR = ".jdfc_instrumented";
    public static final String JDFC_SERIALIZATION_FILE = "jdfc_data.ser";

    public static final List<Integer> JUMP_OPCODES = Arrays.asList(
            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE,
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
            IF_ACMPEQ, IF_ACMPNE,
            IFNULL, IFNONNULL,
            GOTO,
            JSR, // Deprecated but still a jump opcode
            TABLESWITCH,
            LOOKUPSWITCH,
            F_NEW
    );

    public static final List<Integer> RETURN_OPCODES = Arrays.asList(
            IRETURN, // Return int from method
            LRETURN, // Return long from method
            FRETURN, // Return float from method
            DRETURN, // Return double from method
            ARETURN, // Return reference from method
            RETURN  // Return void from method
    );

    public static final UUID ZERO_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
}
