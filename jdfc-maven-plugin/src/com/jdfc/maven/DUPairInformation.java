package com.jdfc.maven;

public class DUPairInformation {

    private String methodName;
    private String name;
    private int defIndex;
    private int useIndex;
    private String type;
    private boolean covered;

    public DUPairInformation(String methodName, String name, int defIndex, int useIndex, String type, boolean covered){
        this.methodName = methodName;
        this.name = name;
        this.defIndex = defIndex;
        this.useIndex = useIndex;
        this.type = type;
        this.covered = covered;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDefIndex() {
        return defIndex;
    }

    public void setDefIndex(int defIndex) {
        this.defIndex = defIndex;
    }

    public int getUseIndex() {
        return useIndex;
    }

    public void setUseIndex(int useIndex) {
        this.useIndex = useIndex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isCovered() {
        return covered;
    }

    public void setCovered(boolean covered) {
        this.covered = covered;
    }
}
