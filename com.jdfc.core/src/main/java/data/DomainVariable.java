package data;

import lombok.Data;
import lombok.NonNull;

import java.util.Objects;

@Data
public class DomainVariable {

    /**
     * Local variable index of domain variable.
     */
    private int index;

    /**
     * Name of the methods owner class (relative path).
     */
    private String className;

    /**
     * Name of the method (ASM).
     */
    private String methodName;

    /**
     * Name of the variable.
     */
    private String name;

    /**
     * Type descriptor of the variable (ASM).
     */
    private String descriptor;


    /**
     *
     * @param className name of the variable's owner class (relative path)
     * @param methodName name of the variable's owner method (ASM) (null for fields)
     * @param name name of the variable
     * @param descriptor  type descriptor of the variable (ASM)
     */
    public DomainVariable(
            int index,
            @NonNull String className,
            @NonNull String methodName,
            @NonNull String name,
            @NonNull String descriptor) {
        this.index = index;
        this.className = className;
        this.methodName = methodName;
        this.name = name;
        this.descriptor = descriptor;
    }

    private DomainVariable() {
        this.index = -1;
        this.className = null;
        this.methodName = null;
        this.name = "0";
        this.descriptor = null;
    }

    public static class ZeroVariable extends DomainVariable{
        public ZeroVariable() {
            super();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainVariable that = (DomainVariable) o;
        return Objects.equals(getClassName(), that.getClassName()) && Objects.equals(getMethodName(), that.getMethodName()) && Objects.equals(getName(), that.getName()) && Objects.equals(getDescriptor(), that.getDescriptor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClassName(), getMethodName(), getName(), getDescriptor());
    }

}
