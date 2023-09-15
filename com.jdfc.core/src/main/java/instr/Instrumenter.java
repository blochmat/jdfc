package instr;

import data.ClassData;
import data.MethodData;
import data.singleton.CoverageDataStore;
import data.visitors.CreateClassDataVisitor;
import graphs.cfg.visitors.classVisitors.LocalVariableClassVisitor;
import graphs.sg.SGCreator;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
public class Instrumenter {

    private final String workDirAbs;
    private final String classesDirAbs;
    private final String sourceDirAbs;

    public void instrumentClass(String classFileAbs) {
        // Create output directory and file
        File classFile = new File(classFileAbs);
        String packagePath = classFile.getAbsolutePath().replace(classesDirAbs, "").replace(classFile.getName(), "");
        File outDir = new File(String.join(File.separator, workDirAbs, ".jdfc_instrumented", packagePath));
        if(!outDir.exists()) {
            outDir.mkdirs();
        }

        // Create class meta data
        ClassMetaData classMetaData = new ClassMetaData(classesDirAbs, sourceDirAbs, classFileAbs);
        CoverageDataStore.getInstance().getClassMetaDataMap().put(classMetaData.getFqn(), classMetaData);

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
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // cw
        final ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        String packageRel = classMetaData.getClassFilePackageRel();
        String className = classMetaData.getName();

        // TODO: skip nested classes

        // Create ClassData and MethodData
        CreateClassDataVisitor createClassDataVisitor = new CreateClassDataVisitor(classNode, classMetaData);
        classReader.accept(createClassDataVisitor, 0);

        UUID classDataId = CoverageDataStore.getInstance().getClassMetaDataMap()
                .get(classMetaData.getFqn()).getClassDataId();
        ClassData classData = CoverageDataStore.getInstance().getClassDataMap().get(classDataId);

        // Find local variables for MethodData
        LocalVariableClassVisitor localVariableVisitor =
                new LocalVariableClassVisitor(classNode, classData);
        classReader.accept(localVariableVisitor, 0);

        // Create CFGs
//        final CFGClassVisitor cfgClassVisitor =
//                new CFGClassVisitor(classNode, classData);
//        classReader.accept(cfgClassVisitor, ClassReader.EXPAND_FRAMES);


//        this.getLocalVariables(classReader, classNode);

//        CFGCreator.createCFGsForClass(classReader, classNode, cData);

        if (CoverageDataStore.getInstance().getPackageDataMap().get(packageRel) != null) {
            ClassData cData = CoverageDataStore.getInstance().getPackageDataMap().get(packageRel).getClassDataByName(className);

            if (cData != null) {
                // Always

                // if intra
//                Set<ProgramVariable> fieldDefinitions = cData.getFieldDefinitions().values().stream()
//                        .flatMap(inner -> inner.values().stream())
//                        .collect(Collectors.toSet());
//                JDFCUtils.logThis(cData.getRelativePath() + "\n" + JDFCUtils.prettyPrintSet(fieldDefinitions), "fieldDefinitions");
                for(MethodData mData : cData.getMethodDataFromStore().values()) {
                    if(mData.getCfg() != null) {
//                        mData.getCfg().getEntryNode().addFieldDefinitions(fieldDefinitions);
                        mData.getCfg().calculateReachingDefinitions();
                        mData.calculateIntraDefUsePairs();
                    } else {
                        System.err.println("ERROR: MethodData.getCfg() returned null! See /target/jdfc/debug/ERROR_JDFCInstrument.log for more info.");
                        if(log.isDebugEnabled()) {
                            File file = JDFCUtils.createFileInDebugDir("ERROR_JDFCInstrument.log", false);
                            try (FileWriter writer = new FileWriter(file, true)) {
                                writer.write(String.format("Class: %s\n", cData.getName()));
                                writer.write(String.format("Method: %s\n", mData.buildInternalMethodName()));
                                writer.write("==============================\n");
                                writer.write("\n");
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    }
                }

                // if inter
                SGCreator.createSGsForClass(cData);
//            ESGCreator.createESGsForClass(cData);

                // if intra

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
                        ClassVisitor iv = new InstrumentationClassVisitor(afterTcv, classNode, cData);


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
                    ClassVisitor cv = new InstrumentationClassVisitor(cw, classNode, cData);

                    // cr -> cv -> cw
                    classReader.accept(cv, ClassReader.EXPAND_FRAMES);
                }

//                ClassReader reader = new ClassReader(cw.toByteArray());
//                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
//
//                if (log.isDebugEnabled()) {
//                    // Debug visitor chain: cr -> beforeTcv -> cv -> afterTCV -> cw
//                    // Byte code is written to two files BEFORE.txt and AFTER.txt.
//                    // Visitor chain is built from back to front
//                    File instrLogDir = JDFCUtils.createFileInInstrDir(classNode.name.replace(File.separator, "."), true);
//                    // afterTcv -> writer
//                    File afterFile = JDFCUtils.createFileIn(instrLogDir, "AFTER_wTc", false);
//                    try (PrintWriter afterWriter = new PrintWriter(new FileWriter(afterFile, true))) {
//                        TraceClassVisitor afterTcv = new TraceClassVisitor(writer, afterWriter);
//
//                        // atcv -> afterTcv -> writer
//                        ClassVisitor atcv = new AddTryCatchClassVisitor(Opcodes.ASM5, afterTcv);
//
//                        reader.accept(atcv, ClassReader.EXPAND_FRAMES);
//                    } catch (IOException ioException) {
//                        ioException.printStackTrace();
//                    }
//                } else {
//                    // Normal visitor chain: cr -> cv -> writer
//                    // cv -> writer
//                    ClassVisitor atcv = new AddTryCatchClassVisitor(Opcodes.ASM5, writer);
//
//                    // cr -> cv -> writer
//                    reader.accept(atcv, ClassReader.EXPAND_FRAMES);
//                }
                CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, new PrintWriter(System.err));
                return cw.toByteArray();
            }
        }
        return cw.toByteArray();
    }
}
