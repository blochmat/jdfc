package com.jdfc.report.html.table;

import com.jdfc.report.html.HTMLElement;

public class Cell extends HTMLElement {

    private final String content;

    public Cell(String pContent){
        content = pContent;
    }

    @Override
    public String render() {
        String tag = "<td>%s</td>";
        return String.format(tag, content);
    }
}
