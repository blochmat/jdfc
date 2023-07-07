package instr;

import data.ClassExecutionData;
import data.MethodData;
import data.ProgramVariable;
import data.singleton.CoverageDataStore;
import graphs.cfg.CFGCreator;
import graphs.esg.ESGCreator;
import graphs.sg.SGCreator;
import instr.classVisitors.InstrumentationClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class JDFCInstrument {

    public byte[] instrument(final ClassReader classReader) {
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // cw
        final ClassWriter cw = new ClassWriter(classReader, 0);
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

                    // cv -> afterTcv -> cw
                    ClassVisitor cv = new InstrumentationClassVisitor(afterTcv, classNode, cData);

                    // beforeTcv -> cv -> afterTcv -> cw
                    File beforeFile = JDFCUtils.createFileIn(instrLogDir, "BEFORE", false);
                    try (PrintWriter beforeWriter = new PrintWriter(new FileWriter(beforeFile, true))) {
                        TraceClassVisitor beforeTcv = new TraceClassVisitor(cv, beforeWriter);
                        // cr -> beforeTcv -> cv -> afterTcv -> cw
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
        }

        return cw.toByteArray();
    }
}
