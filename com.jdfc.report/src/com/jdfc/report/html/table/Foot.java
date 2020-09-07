package com.jdfc.report.html.table;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.report.html.HTMLElement;

public class Foot extends HTMLElement {

    private final Row content;

    public Foot(ExecutionData pData){
        content = new Row("Total", pData);
    }

    @Override
    public String render() {
        String tag = "<tfoot>%s</tfoot>";
        return String.format(tag, content.render());
    }
}
