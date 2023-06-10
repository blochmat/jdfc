package instr;

import data.ClassExecutionData;
import data.singleton.CoverageDataStore;
import graphs.cfg.CFGCreator;
import instr.classVisitors.InstrumentationClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

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

            if (log.isDebugEnabled()) {
                // Debug visitor chain: cr -> beforeTcv -> cv -> afterTCV -> cw
                // Byte code is written to two files BEFORE.txt and AFTER.txt.
                // Visitor chain is built from back to front
                String instrLogDirStr = String.format("%s%starget%sjdfc%slog%sinstrumentation%s%s",
                        System.getProperty("user.dir"), File.separator, File.separator, File.separator, File.separator,
                        File.separator, classNode.name.replace(File.separator, "."));
                File instrLogDir = new File(instrLogDirStr);
                if(instrLogDir.exists() || instrLogDir.mkdirs()) {
                    // afterTcv -> cw
                    String afterFilePathStr = String.format("%s%s%s.txt", instrLogDirStr, File.separator, "AFTER");
                    try (PrintWriter afterWriter = new PrintWriter(new FileWriter(afterFilePathStr, true))) {
                        TraceClassVisitor afterTcv = new TraceClassVisitor(cw, afterWriter);

                        // cv -> afterTcv -> cw
                        ClassVisitor cv = new InstrumentationClassVisitor(afterTcv, classNode, classExecutionData);

                        // beforeTcv -> cv -> afterTcv -> cw
                        String beforeFilePath = String.format("%s%s%s.txt", instrLogDirStr, File.separator, "BEFORE");
                        try (PrintWriter beforeWriter = new PrintWriter(new FileWriter(beforeFilePath, true))) {
                            TraceClassVisitor beforeTcv = new TraceClassVisitor(cv, beforeWriter);
                            // cr -> beforeTcv -> cv -> afterTcv -> cw
                            classReader.accept(beforeTcv, 0);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }

                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
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
