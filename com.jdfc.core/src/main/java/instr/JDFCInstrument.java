package instr;

import data.ClassExecutionData;
import data.MethodData;
import data.ProgramVariable;
import data.io.CoverageDataExport;
import data.singleton.CoverageDataStore;
import graphs.cfg.CFGCreator;
import graphs.esg.ESGCreator;
import graphs.sg.SGCreator;
import instr.classVisitors.AddTryCatchClassVisitor;
import instr.classVisitors.InstrumentationClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class JDFCInstrument {

    /**
     * Load classes for shutdown hook
     */
    private static final Class<?> exportClass = CoverageDataExport.class;

    public JDFCInstrument(String projectDirStr,
                          String buildDirStr,
                          String classesBuildDirStr,
                          String srcDirStr,
                          String classPath) {
        CoverageDataStore.getInstance().saveProjectInfo(projectDirStr, buildDirStr, classesBuildDirStr, srcDirStr);
        File dir = CoverageDataStore.getInstance().getClassesBuildDir();
        Path classesBuildDir = dir.toPath();
        String fileEnding = ".class";
        if(!classPath.equals("")) {

        }
        CoverageDataStore.getInstance().addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), classesBuildDir, fileEnding);

        // add shutdown hook to compute and write results
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            Logger logger = LoggerFactory.getLogger("Shutdown Hook");
//
//            try {
//                logger.info("Export started.");
//                CoverageDataStore.getInstance().exportCoverageData();
//                logger.info("Export finished.");
//            } catch (Exception e) {
//                File jdfcDir = CoverageDataStore.getInstance().getJdfcDir();
//                if (jdfcDir.exists() || jdfcDir.mkdirs()) {
//                    try (FileWriter writer = new FileWriter(JDFCUtils.createFileInJDFCDir("shutdown_error.txt", false), false)) {
//                        String stackTrace = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
//                        writer.write("Exception during shutdown: " + e.getMessage() + "\n");
//                        writer.write(stackTrace);
//                        writer.write("\n");
//                    } catch (IOException ioException) {
//                        ioException.printStackTrace();  // print to console as a last resort
//                    }
//                }
//            }
//        }));
    }

    public byte[] instrument(final ClassReader classReader) {
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // cw
        final ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        ClassExecutionData cData =
                (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(classNode.name).getData();

        if (cData != null) {

            // Always
            CFGCreator.createCFGsForClass(classReader, classNode, cData);

            // if intra
            Set<ProgramVariable> fieldDefinitions = cData.getFieldDefinitions().values().stream()
                    .flatMap(inner -> inner.values().stream())
                    .collect(Collectors.toSet());
            JDFCUtils.logThis(cData.getRelativePath() + "\n" + JDFCUtils.prettyPrintSet(fieldDefinitions), "fieldDefinitions");
            for(MethodData mData : cData.getMethods().values()) {
                mData.getCfg().getEntryNode().addFieldDefinitions(fieldDefinitions);
                mData.getCfg().calculateReachingDefinitions();
                mData.calculateIntraDefUsePairs();
            }

            // if inter
            SGCreator.createSGsForClass(cData);
            ESGCreator.createESGsForClass(cData);

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
//                    CheckClassAdapter cca = new CheckClassAdapter(afterTcv, true);

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

            ClassReader reader = new ClassReader(cw.toByteArray());
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

            if (log.isDebugEnabled()) {
                // Debug visitor chain: cr -> beforeTcv -> cv -> afterTCV -> cw
                // Byte code is written to two files BEFORE.txt and AFTER.txt.
                // Visitor chain is built from back to front
                File instrLogDir = JDFCUtils.createFileInInstrDir(classNode.name.replace(File.separator, "."), true);
                // afterTcv -> writer
                File afterFile = JDFCUtils.createFileIn(instrLogDir, "AFTER_wTc", false);
                try (PrintWriter afterWriter = new PrintWriter(new FileWriter(afterFile, true))) {
                    TraceClassVisitor afterTcv = new TraceClassVisitor(writer, afterWriter);

                    // atcv -> afterTcv -> writer
                    ClassVisitor atcv = new AddTryCatchClassVisitor(Opcodes.ASM5, afterTcv);

                    reader.accept(atcv, ClassReader.EXPAND_FRAMES);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } else {
                // Normal visitor chain: cr -> cv -> writer
                // cv -> writer
                ClassVisitor atcv = new AddTryCatchClassVisitor(Opcodes.ASM5, writer);

                // cr -> cv -> writer
                reader.accept(atcv, ClassReader.EXPAND_FRAMES);
            }
            return writer.toByteArray();
        }

        return cw.toByteArray();
    }
}
