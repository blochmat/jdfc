package data;

import lombok.Data;

@Data
public class DomainVariable {

    /**
     * Name of the methods owner class (relative path).
     */
    private String owner;

    /**
     * Name of the method (ASM).
     */
    private String method;

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
     * @param owner name of the variable's owner class (relative path)
     * @param method name of the variable's owner method (ASM) (null for fields)
     * @param name name of the variable
     * @param descriptor  type descriptor of the variable (ASM)
     */
    public DomainVariable(String owner, String method, String name, String descriptor) {
        this.owner = owner;
        this.method = method;
        this.name = name;
        this.descriptor = descriptor;
    }
}
