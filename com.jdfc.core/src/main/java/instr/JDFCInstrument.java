package instr;

import data.ClassExecutionData;
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

@Slf4j
public class JDFCInstrument {

    public byte[] instrument(final ClassReader classReader) {
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // cw
        final ClassWriter cw = new ClassWriter(classReader, 0);
        ClassExecutionData classExecutionData =
                (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(classNode.name).getData();

        if (classExecutionData != null) {
            CFGCreator.createCFGsForClass(classReader, classNode, classExecutionData);

            SGCreator.createSGsForClass(classExecutionData);

            ESGCreator.createESGsForClass(classExecutionData);

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
                    ClassVisitor cv = new InstrumentationClassVisitor(afterTcv, classNode, classExecutionData);

                    // beforeTcv -> cv -> afterTcv -> cw
                    File beforeFile = JDFCUtils.createFileIn(instrLogDir, "BEFORE", false);
                    try (PrintWriter beforeWriter = new PrintWriter(new FileWriter(beforeFile, true))) {
                        TraceClassVisitor beforeTcv = new TraceClassVisitor(cv, beforeWriter);
                        // cr -> beforeTcv -> cv -> afterTcv -> cw
                        classReader.accept(beforeTcv, 0);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } else {
                // Normal visitor chain: cr -> cv -> cw
                // cv -> cw
                ClassVisitor cv = new InstrumentationClassVisitor(cw, classNode, classExecutionData);

                // cr -> cv -> cw
                classReader.accept(cv, 0);
            }
        }

        return cw.toByteArray();
    }
}
