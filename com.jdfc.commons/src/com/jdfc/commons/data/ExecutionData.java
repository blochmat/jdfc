package com.jdfc.commons.data;

// TODO: Is this class necessary despite for inheritance? Think of something you can add here
public class ExecutionData {

    private int total = 0;
    private int covered = 0;
    private int missed = 0;
    private int methodCount = 0;

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

    public int getMissed() {
        return missed;
    }

    public void setMissed(int missed) {
        this.missed = missed;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

}
