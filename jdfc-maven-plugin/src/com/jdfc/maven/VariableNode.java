package com.jdfc.maven;

import java.util.HashSet;
import java.util.Set;

public class VariableNode extends ExecutionDataNode{

    private String name;
    private String type;
    private Set<CoverageInformation> coverageInformation;

    public VariableNode(String pName, String pType) {
        super();
        this.name = pName;
        this.type = pType;
        this.coverageInformation = new HashSet<CoverageInformation>();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Set<CoverageInformation> getCoverageInformation(){
        return coverageInformation;
    }
}
