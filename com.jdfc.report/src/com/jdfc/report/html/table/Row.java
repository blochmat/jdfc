package com.jdfc.report.html.table;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.report.html.HTMLElement;

import java.util.ArrayList;
import java.util.List;

public class Row extends HTMLElement {
    String tag = "<tr>%s</tr>";
    List<String> entries;

    public Row(String pElement, ExecutionData data) {
        entries = new ArrayList<>();
        entries.add(pElement);
        entries.add(String.valueOf(data.getMethodCount()));
        entries.add(String.valueOf(data.getTotal()));
        entries.add(String.valueOf(data.getCovered()));
        entries.add(String.valueOf(data.getMissed()));
    }

    @Override
    public String render() {
        String cells = "";
        for(String str : entries){
            Cell cell = new Cell(str);
            cells = cells.concat(cell.render());
        }
        return String.format(tag, cells);
    }
}
