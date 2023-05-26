package data;

/**
 *  Coverage data container for packages. Base model for {@code ClassExecutionData}
 */
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

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getFqn() {
        return fqn;
    }

    public String toString() {
        return String.format("Fqn: %s%nMethods: %d%nTotal: %d%nCovered: %d%nRate: %f%n", fqn, methodCount, total, covered, rate);
    }
}
