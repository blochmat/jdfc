package data;

import java.util.Objects;

public class DefUsePair {

    private final ProgramVariable definition;
    private final ProgramVariable usage;
    private boolean covered;

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

    public boolean isCovered() {
        return covered;
    }

    public void setCovered(boolean covered) {
        this.covered = covered;
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
        return String.format("(%n%s,%n%s,%n%b%n)", definition.toString(), usage.toString(), covered);
    }
}
