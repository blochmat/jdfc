package com.jdfc.core.report;

import com.jdfc.core.analysis.JDFCClassVisitor;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.CFGCreator;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM6;

public class ReportClassVisitor extends JDFCClassVisitor {

    private Map<Integer, Integer> opcodes;
    private final int lineNumber;

    public Map<Integer, Integer> getOpcodes() {
        return opcodes;
    }

    public ReportClassVisitor(final ClassNode pNode,
                              final ClassExecutionData pData,
                              final int pLineNumber) {
        super(ASM6, pNode, pData);
        lineNumber = pLineNumber;
    }

//
}
