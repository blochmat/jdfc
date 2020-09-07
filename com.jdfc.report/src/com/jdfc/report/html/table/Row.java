package com.jdfc.report.html.table;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.report.html.HTMLElement;

import java.util.ArrayList;
import java.util.List;

public class Row extends HTMLElement {
    String tag = "<tr>%s</tr>";
    List<String> entries;

    // TODO: Pass list of values to row? Which type?
    public Row(String pElement, ExecutionData data) {
        entries = new ArrayList<>();
        entries.add(pElement);
        entries.add(String.valueOf(data.getMethodCount()));
        entries.add(String.valueOf(data.getTotal()));
        entries.add(String.valueOf(data.getCovered()));
        entries.add(String.valueOf(data.getMissed()));
    }

    public Row(String pElement, int pTotal, int pCovered, int pMissed) {
        entries = new ArrayList<>();
        entries.add(pElement);
        entries.add(String.valueOf(pTotal));
        entries.add(String.valueOf(pCovered));
        entries.add(String.valueOf(pMissed));
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
