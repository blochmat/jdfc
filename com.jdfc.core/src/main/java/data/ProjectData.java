package data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import graphs.cfg.CFG;
import graphs.cfg.LocalVariable;
import instr.ClassMetaData;
import lombok.Data;
import utils.Deserializer;
import utils.JDFCUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A storage singleton for all class data required for the analysis. A tree structure of {@code ExecutionDataNode}
 * instance represent the project structure of the project under test
 */
@Data
public class ProjectData implements Serializable {

    private static final long serialVersionUID = 1L;
    private static volatile ProjectData instance;
    private static volatile ProjectData old;
    private static final Object lock = new Object();

    private File workDir;
    private File buildDir;
    private File classesDir;
    private File jdfcDir;
    private String sourceDirAbs;
    private File jdfcDebugDir;
    private File jdfcDebugInstrDir;
    private File jdfcDebugErrorDir;
    private File jdfcDebugDevLogDir;
    // Filled after test execution

    private int total = 0;
    private int covered = 0;
    private double ratio = 0.0;
    private int methodCount = 0;

    private Map<String, PackageData> packageDataMap;
    private Map<String, ClassMetaData> classMetaDataMap;
    private Map<UUID, ClassData> classDataMap;
    private Map<UUID, MethodData> methodDataMap;
    private Map<UUID, PairData> defUsePairMap;
    private Map<UUID, ProgramVariable> programVariableMap;
    private Set<String> coveredPVarIds;

    /**
     * The keys are variable ids of invoked routines.
     * The values ar variable ids of calling routines.
     */
    private Multimap<UUID, UUID> matchesMap;

//    private final transient ExecutionDataNode<ExecutionData> root;
    private Set<String> testedClassList;
    private Set<String> untestedClassList;

    private static final Class<?> deserializerClass = Deserializer.class;
    private static final Class<?> defUsePairClass = PairData.class;
    private static final Class<?> programVariableClass = ProgramVariable.class;
    private static final Class<?> localVariableClass = LocalVariable.class;
    private static final Class<?> cfgClass = CFG.class;

    private ProjectData(final boolean initHook) {
        JDFCUtils.logThis(Thread.currentThread().getName(), "threads");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Exception occurred in thread: " + t.getName());
            e.printStackTrace();
        });
        this.testedClassList = new HashSet<>();
        this.untestedClassList = new HashSet<>();

        this.packageDataMap = new HashMap<>();
        this.classDataMap = new HashMap<>();
        this.classMetaDataMap = new HashMap<>();
        this.methodDataMap = new HashMap<>();
        this.defUsePairMap = new HashMap<>();
        this.programVariableMap = new HashMap<>();
        this.coveredPVarIds = new HashSet<>();
        this.matchesMap = ArrayListMultimap.create();
        loadOld();

        if (initHook) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try (FileOutputStream fileOut = new FileOutputStream(JDFCUtils.getJDFCSerFileAbs());
                         ObjectOutputStream out = new ObjectOutputStream(fileOut)) {

                        if (old != null) {
                            old.getCoveredPVarIds().addAll(ProjectData.getInstance().getCoveredPVarIds());
                            out.writeObject(old);
                        } else {
                            out.writeObject(ProjectData.getInstance());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        // Consider additional error handling here
                    }
                }
            });
        }
    }

    public static void loadOld() {
        ProjectData.old = Deserializer.deserializeCoverageData(JDFCUtils.getJDFCSerFileAbs());
    }

    public static ProjectData getInstance() {
        synchronized (lock) {
            if(instance == null) {
                String callerClassName = Thread.currentThread().getStackTrace()[2].getClassName();
                String callerMethodName = Thread.currentThread().getStackTrace()[2].getMethodName();
//                JDFCUtils.logThis(callerClassName +"::"+callerMethodName, "ProjectData");
                instance = new ProjectData(true);
            }
        }
        return instance;
    }

    public static void setInstance(ProjectData deserialized) {
        if(instance == null) {
            instance = deserialized;
        }
    }

    public void saveProjectInfo(String projectDirStr,
                                String buildDirStr,
                                String classesBuildDirStr,
                                String srcDirStr) {
        // print uncaught exception
        this.workDir = new File(projectDirStr);
        this.buildDir = new File(buildDirStr);
        this.classesDir = new File(classesBuildDirStr);
        this.sourceDirAbs = srcDirStr;
        this.jdfcDir = new File(String.format("%s%sjdfc", this.buildDir, File.separator));
        this.jdfcDebugDir = new File(String.format("%s%sdebug", this.jdfcDir, File.separator));
        this.jdfcDebugInstrDir = new File(String.format("%s%sinstrumentation", this.jdfcDebugDir, File.separator));
        this.jdfcDebugErrorDir = new File(String.format("%s%serror", this.jdfcDebugDir, File.separator));
        this.jdfcDebugDevLogDir = new File(String.format("%s%slog", this.jdfcDebugDir, File.separator));

    }

//    public static void trackVar(final String cId,
//                                final String mId,
//                                final String pId) {
//        CoverageTracker.getInstance().addVarCoveredEntry(cId, mId, pId);
//    }

    public static void trackVar(final String pId) {
        ProjectData.getInstance().getCoveredPVarIds().add(pId);
//        JDFCUtils.logThis(ProjectData.getInstance().getCoveredPVarIds().toString(), "trackVar");
//        CoverageTracker.getInstance().addVarCoveredEntry(pId);
    }

    public static void trackNewObject(final Object obj,
                                      final String cId,
                                      final String mId) {
//        CoverageTracker.getInstance().trackNewObjectRef(cId, mId, obj);
    }

    public static void trackModifiedObject(final Object obj,
                                           final String cId,
                                           final String mId) {
//        CoverageTracker.getInstance().trackModifiedObjectRef(cId, mId, obj);
    }

    public void exportCoverageData() {
//        Logger logger = LoggerFactory.getLogger("Global");
//        long start = System.currentTimeMillis();
//        logger.info("Coverage data export started.");
//
//
//        // Summary export
//        try {
//            CoverageDataExport.dumpCoverageDataToFile();
//        } catch (ParserConfigurationException | TransformerException e) {
//            throw new RuntimeException(e);
//        }
//
//        // Tested class data export
//        JDFCUtils.logThis(testedClassList.toString(), "tested_classList");
//        for(String className : testedClassList) {
//            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
//            try {
//                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
//            } catch (ParserConfigurationException | TransformerException e) {
//                e.printStackTrace();
//            }
//        }
//
//        // TODO: could be removed
//        // Untested class data export
//        JDFCUtils.logThis(untestedClassList.toString(), "untested_classList");
//        for(String className : untestedClassList) {
//            ClassExecutionData classExecutionData = (ClassExecutionData) findClassDataNode(className).getData();
//            try {
//                CoverageDataExport.dumpClassExecutionDataToFile(classExecutionData);
//            } catch (ParserConfigurationException | TransformerException e) {
//                String stackTrace = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
//                logger.debug("Exception:" + e.getMessage());
//                logger.debug(stackTrace);
//            }
//        }
//
//        long end = System.currentTimeMillis();
//        Duration duration = Duration.ofMillis(end - start);
//        long hours = duration.toHours();
//        duration = duration.minusHours(hours);
//        long minutes = duration.toMinutes();
//        duration = duration.minusMinutes(minutes);
//        long seconds = duration.getSeconds();
//        duration = duration.minusSeconds(seconds);
//        long millis = duration.toMillis();
//
//        String time = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
//        logger.info(String.format("Coverage data export finished. Time: %s", time));
    }

//    public ExecutionDataNode<ExecutionData> findClassDataNode(String pClassName) {
//        ArrayList<String> nodePath = new ArrayList<>(Arrays.asList(pClassName.replace(File.separator, "/").split("/")));
//
//        // root path
//        if (nodePath.size() == 1) {
//            nodePath.add(0, "default");
//        }
//
//        return root.getChildDataRecursive(nodePath);
//    }

//    public void addClassData(String classesAbs, String classFileAbs) {
//        JavaParserHelper javaParserHelper = new JavaParserHelper();
//        File classFile = new File(classFileAbs);
//        String classFileRel = classFileAbs.replace(classesAbs, "");
//        String classFilePackage = classFileRel.replace(classFile.getName(), "").replaceAll("^/|/$", "");
//        String classFileRelNoType = classFileRel.split("\\.")[0].replace(File.separator, "/");
//        String sourceFileRel = classFileRel.replace(".class", ".java");
//        String sourceFileAbs = CoverageDataStore.getInstance().getSourceDirAbs().concat(sourceFileRel);
//        String fqn = classFileRelNoType.replace(File.separator, ".");
//        if(fqn.startsWith(".")) {
//            fqn = fqn.substring(1);
//        }
//
//        File sourceFile = new File(sourceFileAbs);
//        if (sourceFile.exists()) {
//            try {
//                CompilationUnit cu = javaParserHelper.parse(sourceFile);
//                if (!isInterface(cu) && !isEnum(cu) && !isGeneric(cu)) {
//                    UUID id = UUID.randomUUID();
//                    untestedClassList.add(classFileRelNoType);
//                    ClassData cData = new ClassData(fqn, classFile.getName(), id, classFileRelNoType, cu);
//                    classDataMap.put(id, cData);
//
//                    String nameWithoutType = classFile.getName().split("\\.")[0];
//                    if(classFilePackage.equals("")) {
//                        CoverageDataStore.getInstance().packageDataMap.computeIfAbsent("default", k -> new PackageData(classFilePackage));
//                        packageDataMap.get("default").getClassDataIds().add(id);
//                    } else {
//                        packageDataMap.computeIfAbsent(classFilePackage, k -> new PackageData(classFilePackage));
//                        packageDataMap.get(classFilePackage).getClassDataIds().add(id);
//                    }
//                }
//            } catch (FileNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        } else {
//            if(classFileAbs.contains("$")) {
//                System.out.println("Instrumentation of inner class was skipped: " + classFileAbs);
//            } else {
//                throw new RuntimeException("ERROR: Missing source file for " + classFileAbs);
//            }
//        }
//    }

//    public boolean isGeneric(CompilationUnit cu) {
//        List<ClassOrInterfaceDeclaration> list = cu.findAll(ClassOrInterfaceDeclaration.class);
//        for (ClassOrInterfaceDeclaration coi : list) {
//            for (MethodDeclaration m : coi.getMethods()) {
//                if (!m.getTypeParameters().isEmpty()) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    public boolean isInterface(CompilationUnit cu) {
//        // Get a list of all types declared in the file
//        List<TypeDeclaration<?>> types = cu.getTypes();
//        // Check if there's exactly one type
//        if (types.size() == 1) {
//            TypeDeclaration<?> type = types.get(0);
//            // If the single type is an interface
//            return type.isClassOrInterfaceDeclaration() && type.asClassOrInterfaceDeclaration().isInterface();
//        }
//        return false;
//    }
//
//    public boolean isEnum(CompilationUnit cu) {
//        // Get a list of all types declared in the file
//        List<TypeDeclaration<?>> types = cu.getTypes();
//        // Check if there's exactly one type
//        if (types.size() == 1) {
//            TypeDeclaration<?> type = types.get(0);
//            // If the single type is an interface
//            return type.isEnumDeclaration() && type.asClassOrInterfaceDeclaration().isEnumDeclaration();
//        }
//        return false;
//    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeString(out, workDir != null ? workDir.getAbsolutePath() : "");
        writeString(out, buildDir != null ? buildDir.getAbsolutePath() : "");
        writeString(out, classesDir != null ? classesDir.getAbsolutePath() : "");
        writeString(out, jdfcDir != null ? jdfcDir.getAbsolutePath() : "");
        writeString(out, sourceDirAbs);
        writeString(out, jdfcDebugDir != null ? jdfcDebugDir.getAbsolutePath() : "");
        writeString(out, jdfcDebugInstrDir != null ? jdfcDebugInstrDir.getAbsolutePath() : "");
        writeString(out, jdfcDebugErrorDir != null ? jdfcDebugErrorDir.getAbsolutePath() : "");
        writeString(out, jdfcDebugDevLogDir != null ? jdfcDebugDevLogDir.getAbsolutePath() : "");

        out.writeInt(total);
        out.writeInt(covered);
        out.writeDouble(ratio);
        out.writeInt(methodCount);

        writeStringMap(out, packageDataMap);
        writeStringMap(out, classMetaDataMap);
        writeUUIDMap(out, classDataMap);
        writeUUIDMap(out, methodDataMap);
        writeUUIDMap(out, defUsePairMap);
        writeUUIDMap(out, programVariableMap);
        writeStringSet(out, coveredPVarIds);
        writeMultimap(out, matchesMap);
        writeStringSet(out, testedClassList);
        writeStringSet(out, untestedClassList);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String workDirPath = readString(in);
        workDir = workDirPath.isEmpty() ? null : new File(workDirPath);
        String buildDirPath = readString(in);
        buildDir = buildDirPath.isEmpty() ? null : new File(buildDirPath);
        String classesDirPath = readString(in);
        classesDir = classesDirPath.isEmpty() ? null : new File(classesDirPath);
        String jdfcDirPath = readString(in);
        jdfcDir = jdfcDirPath.isEmpty() ? null : new File(jdfcDirPath);
        sourceDirAbs = readString(in);
        String jdfcDebugDirPath = readString(in);
        jdfcDebugDir = jdfcDebugDirPath.isEmpty() ? null : new File(jdfcDebugDirPath);
        String jdfcDebugInstrDirPath = readString(in);
        jdfcDebugInstrDir = jdfcDebugInstrDirPath.isEmpty() ? null : new File(jdfcDebugInstrDirPath);
        String jdfcDebugErrorDirPath = readString(in);
        jdfcDebugErrorDir = jdfcDebugErrorDirPath.isEmpty() ? null : new File(jdfcDebugErrorDirPath);
        String jdfcDebugDevLogDirPath = readString(in);
        jdfcDebugDevLogDir = jdfcDebugDevLogDirPath.isEmpty() ? null : new File(jdfcDebugDevLogDirPath);

        total = in.readInt();
        covered = in.readInt();
        ratio = in.readDouble();
        methodCount = in.readInt();

        packageDataMap = (Map<String, PackageData>) readStringMap(in);
        classMetaDataMap = (Map<String, ClassMetaData>) readStringMap(in);
        classDataMap = (Map<UUID, ClassData>) readUUIDMap(in);
        methodDataMap = (Map<UUID, MethodData>) readUUIDMap(in);
        defUsePairMap = (Map<UUID, PairData>) readUUIDMap(in);
        programVariableMap = (Map<UUID, ProgramVariable>) readUUIDMap(in);
        coveredPVarIds = readStringSet(in);
        matchesMap = readMultimap(in);
        testedClassList = readStringSet(in);
        untestedClassList = readStringSet(in);
    }

    // Helper method to write a String with UTF-8 encoding
    private void writeString(ObjectOutputStream out, String str) throws IOException {
        byte[] bytes = str != null ? str.getBytes(StandardCharsets.UTF_8) : new byte[0];
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    // Helper method to read a String with UTF-8 encoding
    private String readString(ObjectInputStream in) throws IOException {
        int length = in.readInt();
        if (length == 0) return "";
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeStringMap(ObjectOutputStream out, Map<String, ?> map) throws IOException {
        out.writeInt(map.size());
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            writeString(out, entry.getKey());
            out.writeObject(entry.getValue());
        }
    }

    private void writeUUIDMap(ObjectOutputStream out, Map<UUID, ?> map) throws IOException {
        out.writeInt(map.size());
        for (Map.Entry<UUID, ?> entry : map.entrySet()) {
            UUID key = entry.getKey();
            out.writeLong(key.getMostSignificantBits());
            out.writeLong(key.getLeastSignificantBits());
            out.writeObject(entry.getValue());
        }
    }


    // Helper method to read a Map<String, ?>
    private Map<String, ?> readStringMap(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = readString(in);
            Object value = in.readObject();
            map.put(key, value);
        }
        return map;
    }


    private Map<UUID, ?> readUUIDMap(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        Map<UUID, Object> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            long mostSigBits = in.readLong();
            long leastSigBits = in.readLong();
            UUID key = new UUID(mostSigBits, leastSigBits);
            Object value = in.readObject();
            map.put(key, value);
        }
        return map;
    }

    // Helper method to write a Set<String>
    private void writeStringSet(ObjectOutputStream out, Set<String> stringSet) throws IOException {
        out.writeInt(stringSet.size());
        for (String str : stringSet) {
            writeString(out, str);
        }
    }

    // Helper method to read a Set<String>
    private Set<String> readStringSet(ObjectInputStream in) throws IOException {
        int size = in.readInt();
        Set<String> stringSet = new HashSet<>();
        for (int i = 0; i < size; i++) {
            stringSet.add(readString(in));
        }
        return stringSet;
    }

    // Helper method to write a Multimap<UUID, UUID>
    private void writeMultimap(ObjectOutputStream out, Multimap<UUID, UUID> multimap) throws IOException {
        out.writeInt(multimap.size());
        for (Map.Entry<UUID, Collection<UUID>> entry : multimap.asMap().entrySet()) {
            out.writeLong(entry.getKey().getMostSignificantBits());
            out.writeLong(entry.getKey().getLeastSignificantBits());
            Collection<UUID> values = entry.getValue();
            out.writeInt(values.size());
            for (UUID uuid : values) {
                out.writeLong(uuid.getMostSignificantBits());
                out.writeLong(uuid.getLeastSignificantBits());
            }
        }
    }

    // Helper method to read a Multimap<UUID, UUID>
    private Multimap<UUID, UUID> readMultimap(ObjectInputStream in) throws IOException {
        int size = in.readInt();
        Multimap<UUID, UUID> multimap = ArrayListMultimap.create();
        for (int i = 0; i < size; i++) {
            long mostSigBits = in.readLong();
            long leastSigBits = in.readLong();
            UUID key = new UUID(mostSigBits, leastSigBits);
            int valueSize = in.readInt();
            for (int j = 0; j < valueSize; j++) {
                mostSigBits = in.readLong();
                leastSigBits = in.readLong();
                UUID value = new UUID(mostSigBits, leastSigBits);
                multimap.put(key, value);
            }
        }
        return multimap;
    }
}
