package com.jdfc.report.html.table;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;
import com.jdfc.core.analysis.internal.data.PackageExecutionData;

public class Foot extends Row {

    public Foot(ClassExecutionData pData){
        super("Total", pData.getTotal(), pData.getCovered(), pData.getMissed());
    }

    public Foot(PackageExecutionData pData) {
        super("Total", pData);
    }

    @Override
    public String render() {
        String tag = "<tfoot>%s</tfoot>";
        return String.format(tag, super.render());
    }
}
