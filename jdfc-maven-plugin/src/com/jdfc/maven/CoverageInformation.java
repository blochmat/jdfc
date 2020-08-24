package com.jdfc.maven;

public class CoverageInformation {

    private int defIndex;
    private int useIndex;
    private boolean covered;

    public int getDefIndex() {
        return defIndex;
    }

    public int getUseIndex() {
        return useIndex;
    }

    public boolean isCovered() {
        return covered;
    }

    public CoverageInformation(int pDefIndex, int pUseIndex, boolean pCovered){
        this.defIndex = pDefIndex;
        this.useIndex = pUseIndex;
        this.covered = pCovered;
    }
}
