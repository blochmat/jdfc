package utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.Multimap;
import data.ProgramVariable;
import data.ProjectData;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JDFCUtils {

    public static String getOpcode(int value) {
        switch (value) {
            case -1: return "F_NEW";
            case 0: return " NOP ";
            case 1: return "ACONST_NULL";
            case 2: return "ICONST_M1";
            case 3: return "ICONST_0";
            case 4: return "ICONST_1";
            case 5: return "ICONST_2";
            case 6: return "ICONST_3";
            case 7: return "ICONST_4";
            case 8: return "ICONST_5";
            case 9: return "LCONST_0";
            case 10: return "LCONST_1";
            case 11: return "FCONST_0";
            case 12: return "FCONST_1";
            case 13: return "FCONST_2";
            case 14: return "DCONST_0";
            case 15: return "DCONST_1";
            case 16: return "BIPUSH";
            case 17: return "SIPUSH";
            case 18: return "LDC";
            case 21: return "ILOAD";
            case 22: return "LLOAD";
            case 23: return "FLOAD";
            case 24: return "DLOAD";
            case 25: return "ALOAD";
            case 46: return "IALOAD";
            case 47: return "LALOAD";
            case 48: return "FALOAD";
            case 49: return "DALOAD";
            case 50: return "AALOAD";
            case 51: return "BALOAD";
            case 52: return "CALOAD";
            case 53: return "SALOAD";
            case 54: return "ISTORE";
            case 55: return "LSTORE";
            case 56: return "FSTORE";
            case 57: return "DSTORE";
            case 58: return "ASTORE";
            case 79: return "IASTORE";
            case 80: return "LASTORE";
            case 81: return "FASTORE";
            case 82: return "DASTORE";
            case 83: return "AASTORE";
            case 84: return "BASTORE";
            case 85: return "CASTORE";
            case 86: return "SASTORE";
            case 87: return "POP";
            case 88: return "POP2";
            case 89: return "DUP";
            case 90: return "DUP_X1";
            case 91: return "DUP_X2";
            case 92: return "DUP2";
            case 93: return "DUP2_X1";
            case 94: return "DUP2_X2";
            case 95: return "SWAP";
            case 96: return "IADD";
            case 97: return "LADD";
            case 98: return "FADD";
            case 99: return "DADD";
            case 100: return "ISUB";
            case 101: return "LSUB";
            case 102: return "FSUB";
            case 103: return "DSUB";
            case 104: return "IMUL";
            case 105: return "LMUL";
            case 106: return "FMUL";
            case 107: return "DMUL";
            case 108: return "IDIV";
            case 109: return "LDIV";
            case 110: return "FDIV";
            case 111: return "DDIV";
            case 112: return "IREM";
            case 113: return "LREM";
            case 114: return "FREM";
            case 115: return "DREM";
            case 116: return "INEG";
            case 117: return "LNEG";
            case 118: return "FNEG";
            case 119: return "DNEG";
            case 120: return "ISHL";
            case 121: return "LSHL";
            case 122: return "ISHR";
            case 123: return "LSHR";
            case 124: return "IUSHR";
            case 125: return "LUSHR";
            case 126: return "IAND";
            case 127: return "LAND";
            case 128: return "IOR";
            case 129: return "LOR";
            case 130: return "IXOR";
            case 131: return "LXOR";
            case 132: return "IINC";
            case 133: return "I2L";
            case 134: return "I2F";
            case 135: return "I2D";
            case 136: return "L2I";
            case 137: return "L2F";
            case 138: return "L2D";
            case 139: return "F2I";
            case 140: return "F2L";
            case 141: return "F2D";
            case 142: return "D2I";
            case 143: return "D2L";
            case 144: return "D2F";
            case 145: return "I2B";
            case 146: return "I2C";
            case 147: return "I2S";
            case 148: return "LCMP";
            case 149: return "FCMPL";
            case 150: return "FCMPG";
            case 151: return "DCMPL";
            case 152: return "DCMPG";
            case 153: return "IFEQ";
            case 154: return "IFNE";
            case 155: return "IFLT";
            case 156: return "IFGE";
            case 157: return "IFGT";
            case 158: return "IFLE";
            case 159: return "IF_ICMPEQ";
            case 160: return "IF_ICMPNE";
            case 161: return "IF_ICMPLT";
            case 162: return "IF_ICMPGE";
            case 163: return "IF_ICMPGT";
            case 164: return "IF_ICMPLE";
            case 165: return "IF_ACMPEQ";
            case 166: return "IF_ACMPNE";
            case 167: return "GOTO";
            case 168: return "JSR";
            case 169: return "RET";
            case 170: return "TABLESWITCH";
            case 171: return "LOOKUPSWITCH";
            case 172: return "IRETURN";
            case 173: return "LRETURN";
            case 174: return "FRETURN";
            case 175: return "DRETURN";
            case 176: return "ARETURN";
            case 177: return "RETURN";
            case 178: return "GETSTATIC";
            case 179: return "PUTSTATIC";
            case 180: return "GETFIELD";
            case 181: return "PUTFIELD";
            case 182: return "INVOKEVIRTUAL";
            case 183: return "INVOKESPECIAL";
            case 184: return "INVOKESTATIC";
            case 185: return "INVOKEINTERFACE";
            case 186: return "INVOKEDYNAMIC";
            case 187: return "NEW";
            case 188: return "NEWARRAY";
            case 189: return "ANEWARRAY";
            case 190: return "ARRAYLENGTH";
            case 191: return "ATHROW";
            case 192: return "CHECKCAST";
            case 193: return "INSTANCEOF";
            case 194: return "MONITORENTER";
            case 195: return "MONITOREXIT";
            case 197: return "MULTIANEWARRAY";
            case 198: return "IFNULL";
            case 199: return "IFNONNULL";
            default: return "UNKNOWN_OPCODE";
        }
    }

    public static String getASMAccessStr(int value) {
        switch (value){
            case 0: return "protected";
            case 1: return "public";
            case 2: return "private";
            default: return "Unknown Access";
        }
    }

    public static String getJParserAccessStr(int value) {
        switch (value){
            case 0: return "public";
            // TODO
            case 1: return "unknown";
            // TODO
            case 2: return "unknown";
            case 3: return "protected";
            default: return "Unknown Access";
        }
    }

    public static int convertToASMAccess(int value) {
        switch (value){
            case 0: return 1;
            // TODO
            case 1: return Integer.MIN_VALUE;
            // TODO
            case 2: return Integer.MAX_VALUE;
            case 3: return 0;
            default: throw new IllegalArgumentException(String.format("Inconvertible access value: %d", value));
        }
    }

    public static int convertToJParserAccess(int value) {
        switch (value){
            case 0: return 3;
            case 1: return 0;
            case 2: return Integer.MIN_VALUE;
            default: throw new IllegalArgumentException(String.format("Inconvertible access value: %d", value));
        }
    }

    private static String getIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    public static <T> String prettyPrintSet(Set<T> set) {
        StringBuilder stringBuilder = new StringBuilder();
        if(set.isEmpty()) {
            stringBuilder.append("[");
        } else {
            stringBuilder.append("[\n");
        }
        for (T element : set) {
            stringBuilder.append(element.toString()).append("\n");
        }
        stringBuilder.append("]\n");
        return stringBuilder.toString();
    }

    public static String prettyPrintMap(Map<?, ?> map) {
        return prettyPrintMap(map, 0);
    }

    private static String prettyPrintMap(Map<?, ?> map, int indentLevel) {
        StringBuilder sb = new StringBuilder();

        String indent = getIndent(indentLevel);
        sb.append(indent).append("{\n");

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                sb.append(indent).append("  ").append(key).append(" =>\n");
                sb.append(prettyPrintMap((Map<?, ?>) value, indentLevel + 1));
            } else {
                sb.append(indent).append("  ").append(key).append(" => ").append(value).append("\n");
            }
        }

        sb.append(indent).append("}\n");

        return sb.toString();
    }

    public static <K, V> String prettyPrintMultimap(Multimap<K, V> multimap) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (K key : multimap.keySet()) {
            sb.append("  ").append(key.toString()).append(" => ");
            sb.append("[");
            for (V value : multimap.get(key)) {
                sb.append(value.toString()).append(", ");
            }
            sb.append("]\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    public static <T> String prettyPrintArray(T[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (T element : array) {
            sb.append(element.toString()).append(",\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    public static LinkedList<Double> splitInterval(double x, double y, int n) {
        LinkedList<Double> splitPoints = new LinkedList<>();
        double intervalSize = (y - x) / (n + 1);

        for (int i = 1; i <= n; i++) {
            double point = x + (i * intervalSize);
            splitPoints.add(point);
        }

        return splitPoints;
    }

    /**
     * Return everything from string b that is not in string a
     * @param a
     * @param b
     * @return
     */
    public static String getStringDiff(String a, String b) {
        if (a == null && b == null) {
            throw new IllegalArgumentException("String difference can not be determined from two null arguments");
        }

        if(a == null || Objects.equals(a, "")) {
           return b;
        }

        return b.replaceAll(a, "");
    }

    public static String getMethodName(String internalMethodName) {
        return internalMethodName.split(":")[0];
    }

    public static String getReturnType(String descriptor) {
        org.objectweb.asm.Type returnType = org.objectweb.asm.Type.getReturnType(descriptor);
        return returnType.getClassName();
    }

    public static String getTypeName(String descriptor) {
        return org.objectweb.asm.Type.getType(descriptor).getClassName();
    }

    public static String createParamPattern(Set<ProgramVariable> params) {
        List<ProgramVariable> list = new ArrayList<>(params);
        StringBuilder result = new StringBuilder();
        for(ProgramVariable v : list) {
            if (list.indexOf(v) == list.size() - 1) {
                result.append(String.format("\\s*%s\\s+%s\\s*", JDFCUtils.getTypeName(v.getDescriptor()), v.getName()));
            } else {
                result.append(String.format("\\s*%s\\s+%s\\s*,", JDFCUtils.getTypeName(v.getDescriptor()), v.getName()));
            }
        }
        return result.toString();
    }

    public static String createExceptionPattern(List<String> exceptions) {
        StringBuilder result = new StringBuilder();
        for(String ex : exceptions) {
            if (exceptions.indexOf(ex) == exceptions.size() - 1) {
                result.append(String.format("\\%s\\s*", ex));
            } else {
                result.append(String.format("%s\\s*,", ex));
            }
        }
        return result.toString();
    }

    public static List<MethodDeclaration> getMethodDeclList(File javaFile, String cName) throws FileNotFoundException {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        Optional<ClassOrInterfaceDeclaration> ciOptional = cu.getClassByName(cName);
        if (ciOptional.isPresent()) {
            ClassOrInterfaceDeclaration ci = ciOptional.get();
            return ci.getMethods();
        } else {
            throw new IllegalArgumentException("Class is not present in file.");
        }
    }

    public static boolean isNestedClass(String name) {
        return name.contains("$");
    }

    public static boolean isAnonymousInnerClass(String name) {
        Pattern pattern = Pattern.compile("[_a-zA-Z$][_a-zA-Z0-9$]*\\$\\d+");
        Matcher matcher = pattern.matcher(name.replace(".class", ""));

        return matcher.matches();
    }

    public static String innerClassFqnToJVMInternal(String fqn) {
        String[] parts = fqn.split("\\.");
        if (parts.length > 1) {
            String lastPart = parts[parts.length - 1];
            return String.join("/", Arrays.copyOf(parts, parts.length - 1)) + '$' + lastPart;
        } else {
            return fqn;
        }
    }

    public static void logThis(String str, String fileName) {
        if(log.isDebugEnabled()) {
            Thread thread = Thread.currentThread();
            File log = createFileInDebugDevLogDir(fileName, false);
            try (FileWriter writer = new FileWriter(log, true)) {
                writer.write(String.format("ThreadName: %s, ThreadId: %d", thread.getName(), thread.getId()));
                writer.write(getFormattedTimestamp() + " - " + str);
                writer.write("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public static String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }

    public static File createFileInJDFCDir(String fileName, boolean isDir) {
        return JDFCUtils.createFileIn(ProjectData.getInstance().getJdfcDir(), fileName, isDir);
    }

    public static File createFileInDebugDir(String fileName, boolean isDir) {
        return JDFCUtils.createFileIn(ProjectData.getInstance().getJdfcDebugDir(), fileName, isDir);
    }

    public static File createFileInInstrDir(String fileName, boolean isDir) {
        return JDFCUtils.createFileIn(ProjectData.getInstance().getJdfcDebugInstrDir(), fileName, isDir);
    }

    public static File createFileInErrorDir(String fileName, boolean isDir) {
        return JDFCUtils.createFileIn(ProjectData.getInstance().getJdfcDebugErrorDir(), fileName, isDir);
    }

    public static File createFileInDebugDevLogDir(String fileName, boolean isDir) {
        String debugDirStr = String.format("%s%s%s", getPwd(), File.separator, "jdfc-debug");
        return JDFCUtils.createFileIn(debugDirStr, fileName, isDir);
    }

    public static File createFileIn(String dir, String fileName, boolean isDir) {
        return JDFCUtils.createFileIn(new File(dir), fileName, isDir);
    }

    public static File createFileIn(File dir, String fileName, boolean isDir) {
        String fileStr = String.format("%s%s%s", dir, File.separator, fileName);
        File file = new File(fileStr);
        if(isDir) {
            if (file.exists() || file.mkdirs()) {
                return file;
            } else {
                String message = String.format("File could not be created: %s", fileStr);
                throw new RuntimeException(message);
            }
        } else {
            if (dir.exists() || dir.mkdirs()) {
                return file;
            } else {
                String message = String.format("File could not be created: %s", fileStr);
                throw new RuntimeException(message);
            }
        }
    }

    public static String getPwd() {
        String pwd;
        try {
            pwd = new File(".").getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pwd;
    }

    public static String getJDFCDir() {
        String pwd = JDFCUtils.getPwd();
        return String.format("%s%s%s", pwd, File.separator, Constants.JDFC_DIR);
    }

    public static String getJDFCSerFileAbs() {
        return String.format("%s%s%s", getJDFCDir(), File.separator, Constants.JDFC_SERIALIZATION_FILE);
    }
}
