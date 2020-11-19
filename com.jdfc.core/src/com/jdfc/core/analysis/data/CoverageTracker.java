package com.jdfc.core.analysis.data;

import com.jdfc.core.analysis.ifg.data.InstanceVariable;
import com.jdfc.core.analysis.ifg.data.LocalVariable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;

import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.PUTFIELD;

public class CoverageTracker {

    private static CoverageTracker singleton;
    private ClassExecutionData currentClassExecutionData = null;

    public static synchronized CoverageTracker getInstance() {
        if (singleton == null) {
            singleton = new CoverageTracker();
        }
        return singleton;
    }

    public void addLocalVarCoveredEntry(final String pClassName,
                                        final String pInternalMethodName,
                                        final int pVarIndex,
                                        final int pInsnIndex,
                                        final int pLineNumber,
                                        final int pOpcode) {
        updateClassExecutionData(pClassName);
        boolean isDefinition = isDefinition(pOpcode);
        LocalVariable localVariable = currentClassExecutionData.findLocalVariable(pInternalMethodName, pVarIndex);

        if (localVariable != null) {
            ProgramVariable programVariable =
                    ProgramVariable.create(null, localVariable.getName(), localVariable.getDescriptor(),
                            pInternalMethodName, pInsnIndex, pLineNumber, false, isDefinition);
            programVariable.setReference(isHolder(currentClassExecutionData, programVariable));
            addCoveredEntry(pInternalMethodName, currentClassExecutionData, programVariable);
        }
    }

    public void addInstanceVarCoveredEntry(final String pClassName,
                                           final String pOwner,
                                           final String pInternalMethodName,
                                           final String pVarName,
                                           final String pVarDesc,
                                           final int pInsnIndex,
                                           final int pLineNumber,
                                           final int pOpcode) {
        updateClassExecutionData(pClassName);
        boolean isDefinition = isDefinition(pOpcode);
        ProgramVariable programVariable = ProgramVariable.create(pOwner, pVarName, pVarDesc, pInternalMethodName,
                pInsnIndex, pLineNumber, false, isDefinition);
        InstanceVariable instanceVariable = currentClassExecutionData.findInstanceVariable(programVariable);
        if (instanceVariable != null) {
            addCoveredEntry(pInternalMethodName, currentClassExecutionData, programVariable);
            addCoveredEntry(pInternalMethodName, currentClassExecutionData, instanceVariable.getHolder());
        }
    }

    private void updateClassExecutionData(final String pClassName) {
        if (currentClassExecutionData == null || !currentClassExecutionData.getRelativePath().equals(pClassName)) {
            currentClassExecutionData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        }
    }

    static boolean isHolder(final ClassExecutionData pData,
                            final ProgramVariable pVariable) {
        for (InstanceVariable element : pData.getInstanceVariables()) {
            ProgramVariable holder = element.getHolder();
            if (holder.getOwner() == null && pVariable.getOwner() == null
                    && holder.getName().equals(pVariable.getName())
                    && holder.getDescriptor().equals(pVariable.getDescriptor())
                    && holder.getLineNumber() == pVariable.getLineNumber()
                    && holder.getInstructionIndex() == pVariable.getInstructionIndex()) {
                return true;
            }
        }
        return false;
    }

    private static void addCoveredEntry(String methodNameDesc, ClassExecutionData classExecutionData, ProgramVariable programVariable) {
        if (programVariable != null) {
            Map<String, Set<ProgramVariable>> coveredList = classExecutionData.getVariablesCovered();
            coveredList.get(methodNameDesc).add(programVariable);
        }
    }

    static boolean isDefinition(final int pOpcode) {
        switch (pOpcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
            case PUTFIELD:
                return true;
            default:
                return false;
        }
    }
}
