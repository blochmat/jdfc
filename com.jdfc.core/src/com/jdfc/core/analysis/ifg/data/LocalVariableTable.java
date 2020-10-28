package com.jdfc.core.analysis.ifg.data;

import com.google.common.collect.Maps;
import com.jdfc.commons.utils.PrettyPrintMap;

import java.util.Map;
import java.util.Optional;

/**
 * Implements a local variable table similar to the one of a Java class file.
 *
 * @see LocalVariable
 */
public class LocalVariableTable {

    private final Map<Integer, LocalVariable> localVariableTable;

    public LocalVariableTable() {
        localVariableTable = Maps.newLinkedHashMap();
    }

    void addEntry(final int pIndex, final LocalVariable pLocalVariable) {
        if (!localVariableTable.containsKey(pIndex)) {
            localVariableTable.put(pIndex, pLocalVariable);
        }
    }

    public Optional<LocalVariable> getEntry(final int pIndex) {
        if (localVariableTable.containsKey(pIndex)) {
            return Optional.of(localVariableTable.get(pIndex));
        } else {
            return Optional.empty();
        }
    }

    public boolean containsEntry(final String pName, final String pDescriptor) {
        return localVariableTable.values().stream()
                .anyMatch(x -> x.getName().equals(pName) && x.getDescriptor().equals(pDescriptor));
    }

    public int size() {
        return  localVariableTable.size();
    }

    public void print() {
        System.out.println(new PrettyPrintMap<>(this.localVariableTable));
    }
}