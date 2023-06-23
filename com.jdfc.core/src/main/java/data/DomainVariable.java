package data;

import lombok.Data;
import lombok.NonNull;

@Data
public class DomainVariable {

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
    public DomainVariable(@NonNull String className,
                          @NonNull String methodName,
                          @NonNull String name,
                          @NonNull String descriptor) {
        this.className = className;
        this.methodName = methodName;
        this.name = name;
        this.descriptor = descriptor;
    }

    private DomainVariable() {
        this.className = null;
        this.methodName = null;
        this.name = null;
        this.descriptor = null;
    }

    public static class ZeroVariable extends DomainVariable{
        public ZeroVariable() {
            super();
        }
    }
}
