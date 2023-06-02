package data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 *  Coverage data container for packages. Base model for {@code ClassExecutionData}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionData {

    private int total = 0;
    private int covered = 0;
    private int methodCount = 0;
    private double rate = 0.0;
    private String fqn = "";
    private String name = "";
    private String parentFqn = "";

    public ExecutionData(String fqn, String name) {
        this.fqn = fqn;
        this.name = name;
        String temp = fqn.replaceAll(name, "");
        if (!Objects.equals(temp,"")) {
            int i = temp.length() - 1;
            this.parentFqn = temp.substring(0, i);
        }
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCovered() {
        return covered;
    }

    public void setCovered(int covered) {
        this.covered = covered;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getFqn() {
        return fqn;
    }

    public String getName() {
        return name;
    }

    public String getParentFqn() {
        return parentFqn;
    }

    public String toString() {
        return String.format("ParentFqn: %s%nFqn: %s%nMethods: %d%nTotal: %d%nCovered: %d%nRate: %f%n", parentFqn, fqn, methodCount, total, covered, rate);
    }
}
