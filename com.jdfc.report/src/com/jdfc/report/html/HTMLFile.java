package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.report.html.table.Table;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HTMLFile {

    private final String main;
    private String head;
    private String body;
    List<HTMLElement> content;

    public HTMLFile(){
        this.main = "<!DOCTYPE html><html>%s%s</html>";
        this.head = "<head>%s</head>";
        this.body = "<body>%s</body>";
        content = new ArrayList<>();
    }

    public String render(){
        body = renderBody();
        return String.format(main, head, body);
    }

    private String renderBody() {
        String render = "";
        for(HTMLElement el : content){
            render = render.concat(el.render());
        }
        return String.format(body, render);
    }

    public void fillHeader(String str){
        // mainly style and title
        head = String.format(head, str);
    }

    public void fillBody(String str) {
        // contents are tables or something
        body = String.format(body, str);
    }

    public void addTable(Map<String, ExecutionData> pClassFileDataMap) {
        Table table = new Table();
        for(Map.Entry<String, ExecutionData> entry : pClassFileDataMap.entrySet()) {
            table.addRow(entry.getKey(), entry.getValue());
        }
        content.add(table);
    }
}
