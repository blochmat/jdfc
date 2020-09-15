package com.jdfc.report.html.table;

import com.jdfc.report.html.HTMLElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Row extends HTMLElement {
    String tag = "<tr>%s</tr>";
    String link;
    boolean isLinked = false;
    List<String> entries;

    public Row(String[] pEntries) {
        entries = new ArrayList<>();
        entries.addAll(Arrays.asList(pEntries));
    }

    public Row(String[] pEntries, String pLink) {
        link = pLink;
        entries = new ArrayList<>();
        entries.addAll(Arrays.asList(pEntries));
    }

    @Override
    public String render() {
        String cells = "";
        for(String str : entries){
            if(link != null && !isLinked){
                str = String.format("<a href=\"%s\">%s</a>", link, str);
                isLinked = true;
            }
            Cell cell = new Cell(str);
            cells = cells.concat(cell.render());
        }
        return String.format(tag, cells);
    }
}
