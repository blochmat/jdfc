package graphs.sg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterVariable {
    private String owner;
    private String method;
    private String name;
    private String descriptor;
}
