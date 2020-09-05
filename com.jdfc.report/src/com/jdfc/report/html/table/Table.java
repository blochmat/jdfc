package com.jdfc.report.html.table;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.report.html.HTMLElement;

import java.util.ArrayList;
import java.util.List;

public class Table extends HTMLElement {

    //TODO: Maybe head foot body as List<Row>
    List<String> columns;
    List<Row> rows;
    String tag = "<table>%s</table>";

    public Table() {
        columns = new ArrayList<>();
        rows = new ArrayList<>();
        columns.add("element");
        columns.add("method count");
        columns.add("total");
        columns.add("covered");
        columns.add("missed");
    }

    private String createTableHead() {
        String headTag = "<thead>%s</thead>";
        String render = "";
        for(String str : columns) {
            Cell cell = new Cell(str);
            render = render.concat(cell.render());
        }
        return String.format(headTag, render);
    }

    private String createTableFoot(){
        String footTag = "<tfoot>%s</tfoot>";
        String render = "";
        for(String str : columns) {
            Cell cell = new Cell("n.a.");
            render = render.concat(cell.render());
        }
        return String.format(footTag, render);
    }

    private String createTableBody(){
        String bodyTag = "<tbody>%s</tbody>";
        String render = "";
        for(Row row : rows) {
            render = render.concat(row.render());
        }
        return String.format(bodyTag, render);
    }

    public void addRow(String element, ExecutionData pData){
        Row row = new Row(element, pData);
        rows.add(row);
    }

    @Override
    public String render(){
        String head = createTableHead();
        String foot = createTableFoot();
        String body = createTableBody();

        return String.format(tag,
                String.format("%s%s%s", head, foot, body));
    }
}
