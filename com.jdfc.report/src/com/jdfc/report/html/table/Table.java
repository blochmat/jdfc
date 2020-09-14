package com.jdfc.report.html.table;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.data.PackageExecutionData;
import com.jdfc.report.html.HTMLElement;

import java.util.ArrayList;
import java.util.List;

public class Table extends HTMLElement {

    //TODO: Maybe head foot body as List<Row>
    List<String> columns;
    List<Row> rows;
    Foot foot;
    String tag = "<table>%s</table>";

    public Table(List<String> pColumns) {
        columns = pColumns;
        rows = new ArrayList<>();
    }

    private String createTableHead() {
        if (columns != null) {
            String headTag = "<thead>%s</thead>";
            String render = "";
            for(String str : columns) {
                Cell cell = new Cell(str);
                render = render.concat(cell.render());
            }
            return String.format(headTag, render);
        }
        return "";
    }

    private String createTableBody(){
        String bodyTag = "<tbody>%s</tbody>";
        String render = "";
        for(Row row : rows) {
            render = render.concat(row.render());
        }
        return String.format(bodyTag, render);
    }

    public void addRow(Row row){
        rows.add(row);
    }

    public void addRow(String element, ExecutionData pData){
        rows.add(new Row(element, pData));
    }

    public void addRow(String pElement, int pTotal, int pCovered, int pMissed) {
        rows.add(new Row(pElement, pTotal, pCovered, pMissed));
    }

    public void addTableFoot(ExecutionData pData) {
        if (pData instanceof ClassExecutionData) {
            foot = new Foot((ClassExecutionData) pData);
        } else {
            foot = new Foot((PackageExecutionData) pData);
        }
    }

    @Override
    public String render(){
        String head = createTableHead();
        String body = createTableBody();
        String renderedFoot;
        if (foot != null) {
            renderedFoot = this.foot.render();
        } else {
            renderedFoot = "";
        }
        return String.format(tag,
                String.format("%s%s%s", head, renderedFoot, body));
    }
}
