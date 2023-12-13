package instr;

import algos.TabulationAlgorithm;
import data.*;
import data.visitors.CreateClassDataVisitor;
import graphs.cfg.visitors.classVisitors.CFGClassVisitor;
import graphs.cfg.visitors.classVisitors.LocalVariableClassVisitor;
import graphs.esg.ClassEsgCreator;
import graphs.sg.SGCreator;
import graphs.sg.nodes.SGNode;
import instr.classVisitors.InstrumentationClassVisitor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import utils.JDFCUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.ACC_ENUM;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

@Slf4j
@AllArgsConstructor
public class Instrumenter {

    private final String workDirAbs;
    private final String classesDirAbs;
    private final String sourceDirAbs;

    public void instrumentClass(String classFileAbs) {
        // Create output directory and file
        log.info("Instrument: " + classFileAbs);
        File classFile = new File(classFileAbs);
        String packagePath = classFile.getAbsolutePath().replace(classesDirAbs, "").replace(classFile.getName(), "");
        File outDir = new File(String.join(File.separator, workDirAbs, ".jdfc_instrumented", packagePath));
        if(!outDir.exists()) {
            outDir.mkdirs();
        }

        // Create class meta data
        ClassMetaData classMetaData = new ClassMetaData(classesDirAbs, sourceDirAbs, classFileAbs);

        // Instrument and write to file
        String outPath = String.join(File.separator, outDir.getAbsolutePath(), classFile.getName());
        try (FileOutputStream fos = new FileOutputStream(outPath)){
            byte[] classFileBuffer = Files.readAllBytes(classFile.toPath());
            ClassReader cr = new ClassReader(classFileBuffer);
            byte[] instrumented = this.instrument(cr, classMetaData);
            fos.write(instrumented);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] instrumentClass(byte[] classFileBuffer, String classFileAbs) {
//        CoverageDataStore.getInstance().addClassData(classesDirAbs, classFileAbs);
        ClassReader cr = new ClassReader(classFileBuffer);
        ClassMetaData classMetaData = new ClassMetaData(classesDirAbs, sourceDirAbs, classFileAbs);
        return this.instrument(cr, classMetaData);
    }

    public List<File> loadClassFiles() {
        List<File> classFiles = new ArrayList<>();
        try {
            Files.walkFileTree(Paths.get(classesDirAbs), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".class")) {
                        classFiles.add(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classFiles;
    }

    public byte[] instrument(ClassReader classReader, ClassMetaData classMetaData) {
//        this.loadAllRequiredClasses(classMetaData);
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        final ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

//        // skip instrumentation for nested classes
//        if(classMetaData.getClassFileAbs().contains("$")) {
//            System.out.println("Skipping instrumentation of inner class: " + classMetaData.getClassFileAbs());
//            return cw.toByteArray();
//        }

        // skip instrumentation for nested classes
        if(this.isInterface(classNode.access)) {
            System.out.println("Skipping instrumentation of interface: " + classMetaData.getClassFileAbs());
            classReader.accept(cw, 0);
            CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, new PrintWriter(System.err));
            return cw.toByteArray();
        }

        // skip instrumentation for nested classes
        if(this.isEnum(classNode.access)) {
            System.out.println("Skipping instrumentation of enum: " + classMetaData.getClassFileAbs());
            classReader.accept(cw, 0);
            CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, new PrintWriter(System.err));
            return cw.toByteArray();
        }

        // skip instrumentation for nested classes
        if(this.isAnonymousInnerClass(classMetaData.getFqn())) {
            System.out.println("Skipping instrumentation of anonymous inner class: " + classMetaData.getClassFileAbs());
            classReader.accept(cw, 0);
            CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, new PrintWriter(System.err));
            return cw.toByteArray();
        }

        // Add class meta data
        ProjectData.getInstance().getClassMetaDataMap().put(classMetaData.getFqn(), classMetaData);

        // Create PackageData of class
        String packageRel = classMetaData.getClassFilePackageRel();
        if (ProjectData.getInstance().getPackageDataMap().get(packageRel) == null) {
            PackageData packageData = new PackageData(packageRel);
            ProjectData.getInstance().getPackageDataMap().put(packageRel, packageData);
        }

        // Create ClassData and MethodData of class
        CreateClassDataVisitor createClassDataVisitor = new CreateClassDataVisitor(classNode, classMetaData);
        classReader.accept(createClassDataVisitor, 0);

        // Get ClassData
        UUID classDataId = ProjectData.getInstance().getClassMetaDataMap().get(classMetaData.getFqn()).getClassDataId();
        ClassData classData = ProjectData.getInstance().getClassDataMap().get(classDataId);

        // Find local variables for all methods
        LocalVariableClassVisitor localVariableVisitor = new LocalVariableClassVisitor(classNode, classData);
        classReader.accept(localVariableVisitor, ClassReader.EXPAND_FRAMES);

        // Create CFGs for all methods
        final CFGClassVisitor cfgClassVisitor = new CFGClassVisitor(classNode, classData);
        classReader.accept(cfgClassVisitor, ClassReader.EXPAND_FRAMES);

        // Calculate DefUsePairs
        // Todo: This needs to stay here.
        for(MethodData mData : classData.getMethodDataFromStore().values()) {
            if(mData.getCfg() != null) {
                mData.getCfg().calculateReachingDefinitions();
                mData.calculateIntraProcDefUsePairs();
            } else {
                System.err.println("ERROR: MethodData.getCfg() returned null! See /target/jdfc/debug/ERROR_JDFCInstrument.log for more info.");
                if(log.isDebugEnabled()) {
                    File file = JDFCUtils.createFileInDebugDir("ERROR_JDFCInstrument.log", false);
                    try (FileWriter writer = new FileWriter(file, true)) {
                        writer.write(String.format("Class: %s\n", classData.getClassMetaData().getName()));
                        writer.write(String.format("Method: %s\n", mData.buildInternalMethodName()));
                        writer.write("==============================\n");
                        writer.write("\n");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        }

        // Create pVarMaps
        for(MethodData mData : classData.getMethodDataFromStore().values()) {
            if(mData.getCfg() != null) {
                mData.createIndexDefinitionsMap();
            } else {
                System.err.println("ERROR: MethodData.getCfg() returned null! See /target/jdfc/debug/ERROR_JDFCInstrument.log for more info.");
            }
        }

        // Create SGs for all methods of class
        SGCreator sgCreator = new SGCreator();
        sgCreator.createSGsForClass(classData);
//
//        // Create ESGs for all methods of class
        ClassEsgCreator classEsgCreator = new ClassEsgCreator();
        classEsgCreator.createESGsForClass(classData);

        // TODO
        // Compute inter-procedural pairs
        for (MethodData mData : classData.getMethodDataFromStore().values()) {
            if(log.isDebugEnabled() && !mData.getName().contains("defineAStatic") && mData.getName().contains("defineA")) {
                System.out.println();
            }
            TabulationAlgorithm tabulationAlgorithm = new TabulationAlgorithm(mData.getEsg());
            Map<Integer, Set<UUID>> MVP = tabulationAlgorithm.execute();
//            mData.calculateInterProcDefUsePairs(MVP);
            if(log.isDebugEnabled() && !mData.getName().contains("defineAStatic") && mData.getName().contains("defineA")) {
                System.out.println();
            }
        }

        // Tracking instrumentation
        if (log.isDebugEnabled()) {
            // Debug visitor chain: cr -> beforeTcv -> cv -> afterTCV -> cw
            // Byte code is written to two files BEFORE.txt and AFTER.txt.
            // Visitor chain is built from back to front
            File instrLogDir = JDFCUtils.createFileInInstrDir(classNode.name.replace(File.separator, "."), true);
            // afterTcv -> cw
            File afterFile = JDFCUtils.createFileIn(instrLogDir, "AFTER", false);
            try (PrintWriter afterWriter = new PrintWriter(new FileWriter(afterFile, true))) {
                TraceClassVisitor afterTcv = new TraceClassVisitor(cw, afterWriter);
//                        CheckClassAdapter cca = new CheckClassAdapter(afterTcv, true);

                // iv -> afterTcv -> cw
                ClassVisitor iv = new InstrumentationClassVisitor(afterTcv, classNode, classData);

                // beforeTcv -> iv -> afterTcv -> cw
                File beforeFile = JDFCUtils.createFileIn(instrLogDir, "BEFORE", false);
                try (PrintWriter beforeWriter = new PrintWriter(new FileWriter(beforeFile, true))) {
                    TraceClassVisitor beforeTcv = new TraceClassVisitor(iv, beforeWriter);
                    // cr -> beforeTcv -> iv -> afterTcv -> cw
                    classReader.accept(beforeTcv, ClassReader.EXPAND_FRAMES);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } else {
            // Normal visitor chain: cr -> cv -> cw
            // cv -> cw
            ClassVisitor cv = new InstrumentationClassVisitor(cw, classNode, classData);

            // cr -> cv -> cw
            classReader.accept(cv, ClassReader.EXPAND_FRAMES);
        }

        // Surround everything with a try-catch
//        ClassReader reader = new ClassReader(cw.toByteArray());
//        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
//
//        if (log.isDebugEnabled()) {
//            // Debug visitor chain: cr -> beforeTcv -> cv -> afterTCV -> cw
//            // Byte code is written to two files BEFORE.txt and AFTER.txt.
//            // Visitor chain is built from back to front
//            File instrLogDir = JDFCUtils.createFileInInstrDir(classNode.name.replace(File.separator, "."), true);
//            // afterTcv -> writer
//            File afterFile = JDFCUtils.createFileIn(instrLogDir, "AFTER_wTc", false);
//            try (PrintWriter afterWriter = new PrintWriter(new FileWriter(afterFile, true))) {
//                TraceClassVisitor afterTcv = new TraceClassVisitor(writer, afterWriter);
//
//                // atcv -> afterTcv -> writer
//                ClassVisitor atcv = new AddTryCatchClassVisitor(ASM5, afterTcv);
//
//                reader.accept(atcv, ClassReader.EXPAND_FRAMES);
//            } catch (IOException ioException) {
//                ioException.printStackTrace();
//            }
//        } else {
//            // Normal visitor chain: cr -> cv -> writer
//            // cv -> writer
//            ClassVisitor atcv = new AddTryCatchClassVisitor(ASM5, writer);
//
//            // cr -> cv -> writer
//            reader.accept(atcv, ClassReader.EXPAND_FRAMES);
//        }

        // Check if generated bytecode is correct
        CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, new PrintWriter(System.err));
        return cw.toByteArray();
    }

//    private void loadAllRequiredClasses(ClassMetaData classMetaData) {
//        try {
//            String classPath = System.getProperty("java.class.path");
//            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
//            Class<?> clazz = Class.forName(classMetaData.getFqn(), true, getClass().getClassLoader());
////            Constructor<?> constructor = clazz.getDeclaredConstructor();
////            constructor.setAccessible(true);
////            constructor.newInstance();
//
//            Class<?>[] innerClasses = clazz.getDeclaredClasses();
//            for (Class<?> innerClass : innerClasses) {
//                Class<?> loadedInnerClass = Class.forName(innerClass.getName(), true, systemClassLoader);
//                this.loadSuperClasses(loadedInnerClass);
//            }
//
//            Class<?> superClass = clazz.getSuperclass();
//            this.loadSuperClasses(superClass);
//
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void loadSuperClasses(Class<?> clazz) throws ClassNotFoundException {
//        Class<?> superClass = clazz.getSuperclass();
//        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
//        while (superClass != null) {
//            Class<?> loadedSuperClass = Class.forName(superClass.getName(), true, systemClassLoader);
//            superClass = loadedSuperClass.getSuperclass();
//        }
//    }

    private boolean isAnonymousInnerClass(String fqn) {
        String[] parts = fqn.split("\\$");
        if (parts.length == 1) {
            return false;
        } else if (parts.length == 2) {
            Pattern pattern = Pattern.compile("-?\\d+");
            Matcher matcher = pattern.matcher(parts[1]);

            return matcher.matches();
        } else {
            throw new IllegalArgumentException("Inner, inner class: " + fqn);
        }
    }

    private boolean isInterface(int access) {
        return (access & ACC_INTERFACE) != 0;
    }

    private boolean isEnum(int access) {
        return (access & ACC_ENUM) != 0;
    }
}
