package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a program variable that is identified by its name and type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramVariable implements Comparable<Object> {

    private String owner;
    private String name;
    private String desc;
    private int insnIdx;
    private int lineNr;
    private boolean isDef;
    private boolean isCov;

//    public ProgramVariable clone() throws CloneNotSupportedException {
//        return (ProgramVariable) super.clone();
//    }
//
    @Override
    public int compareTo(Object pOther) {
        if (pOther == null) {
            throw new NullPointerException("Can't compare to null.");
        }
        ProgramVariable that = (ProgramVariable) pOther;

        if (this.equals(that)) {
            return 0;
        }
        if (this.getLineNr() == that.getLineNr()) {
            if (this.getInsnIdx() < that.getInsnIdx()) {
                return -1;
            } else {
                return 1;
            }
        } else {
            if (this.getLineNr() < that.getLineNr()) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
