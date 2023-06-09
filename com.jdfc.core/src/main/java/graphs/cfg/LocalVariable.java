package graphs.cfg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a local variable of a method as it is in the class file's local variable table. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalVariable {
    private int index;
    private String name;
    private String descriptor;
    private String signature;
}
