package com.jdfc.core.analysis.ifg.data;

import java.util.Objects;

public class DefUsePair {

    private final ProgramVariable definition;
    private final ProgramVariable usage;

    public DefUsePair(ProgramVariable definition, ProgramVariable usage) {
        this.definition = definition;
        this.usage = usage;
    }

    public ProgramVariable getDefinition() {
        return definition;
    }

    public ProgramVariable getUsage() {
        return usage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefUsePair that = (DefUsePair) o;
        return definition.equals(that.definition) && Objects.equals(usage, that.usage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition, usage);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", definition.toString(), usage.toString());
    }
}
