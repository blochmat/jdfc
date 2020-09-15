package com.jdfc.report.html.table;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.core.analysis.data.ClassExecutionData;
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

    private String createHead() {
        if (columns != null) {
            String headTag = "<thead>%s</thead>";
            String render = "";
            for (String str : columns) {
                Cell cell = new Cell(str);
                render = render.concat(cell.render());
            }
            return String.format(headTag, render);
        }
        return "";
    }

    private String createBody() {
        String bodyTag = "<tbody>%s</tbody>";
        String render = "";
        for (Row row : rows) {
            render = render.concat(row.render());
        }
        return String.format(bodyTag, render);
    }

    public void addRow(Row row) {
        rows.add(row);
    }

    public void addRow(String[] entries, String link) {
        rows.add(new Row(entries, link));
    }

    public void createFoot(ExecutionData pData) {
        String[] entries;
        if (pData instanceof ClassExecutionData) {
            entries = new String[]
                    {"Total",
                            String.valueOf(pData.getTotal()),
                            String.valueOf(pData.getCovered()),
                            String.valueOf(pData.getMissed())};
        } else {
            entries = new String[]
                    {"Total",
                            String.valueOf(pData.getMethodCount()),
                            String.valueOf(pData.getTotal()),
                            String.valueOf(pData.getCovered()),
                            String.valueOf(pData.getMissed())
                    };
        }
        foot = new Foot(entries);
    }

    @Override
    public String render() {
        String head = createHead();
        String body = createBody();
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
