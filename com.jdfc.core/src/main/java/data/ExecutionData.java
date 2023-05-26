package data;

/**
 *  Coverage data container for packages. Base model for {@code ClassExecutionData}
 */
public class ExecutionData {

    private int total = 0;
    private int covered = 0;
    private int methodCount = 0;
    private String fqn = "";

    public ExecutionData(String fqn) {
        this.fqn = fqn;
    }

    public int getTotal() {
        return total;
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

    public String getFqn() {
        return fqn;
    }

    public void setFqn(String fqn) {
        this.fqn = fqn;
    }

    public String toString() {
        return String.format("Fqn: %s%nMethods: %d%nTotal: %d%nCovered: %d%n", fqn, methodCount, total, covered);
    }
}
