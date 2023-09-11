package instr;

import data.ClassExecutionData;
import data.MethodData;
import data.ProgramVariable;
import data.io.CoverageDataExport;
import data.singleton.CoverageDataStore;
import graphs.cfg.CFGCreator;
import instr.classVisitors.InstrumentationClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class JDFCInstrument {

    /**
     * Load classes for shutdown hook
     */
    private static final Class<?> exportClass = CoverageDataExport.class;

    public byte[] instrument(final ClassReader classReader) {
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // cw
        final ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        String packageRel = this.getPackage(classNode);
        String className = this.getClassName(classNode);

        if (CoverageDataStore.getInstance().getProjectData().get(packageRel) != null) {
            ClassExecutionData cData = CoverageDataStore.getInstance().getProjectData().get(packageRel).get(className);

            if (cData != null) {
                // Always
                CFGCreator.createCFGsForClass(classReader, classNode, cData);

                // if intra
                Set<ProgramVariable> fieldDefinitions = cData.getFieldDefinitions().values().stream()
                        .flatMap(inner -> inner.values().stream())
                        .collect(Collectors.toSet());
                JDFCUtils.logThis(cData.getRelativePath() + "\n" + JDFCUtils.prettyPrintSet(fieldDefinitions), "fieldDefinitions");
                for(MethodData mData : cData.getMethods().values()) {
                    if(mData.getCfg() != null) {
                        mData.getCfg().getEntryNode().addFieldDefinitions(fieldDefinitions);
                        mData.getCfg().calculateReachingDefinitions();
                        mData.calculateIntraDefUsePairs();
                        CoverageDataStore.getInstance().getProgramVariableMap().putAll(mData.getProgramVariables());
                        CoverageDataStore.getInstance().getDefUsePairMap().putAll(mData.getPairs());
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
//            SGCreator.createSGsForClass(cData);
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

    private String getPackage(ClassNode classNode) {
        String[] components = classNode.name.split("/");
        String[] packageComponents = Arrays.copyOf(components, components.length - 1);
        return String.join("/", packageComponents);
    }

    private String getClassName(ClassNode classNode) {
        String[] components = classNode.name.split("/");
        return components[components.length - 1];
    }
}
