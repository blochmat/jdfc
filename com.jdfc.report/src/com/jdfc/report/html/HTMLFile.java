package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.report.html.table.Table;

import java.util.*;

public class HTMLFile {

    private final String main;
    private String head;
    private String body;
    private List<HTMLElement> content;

    public HTMLFile(){
        this.main = "<!DOCTYPE html><html>%s</br>%s</html>";
        this.head = "<head>%s</head>";
        this.body = "<body>%s</body>";
        content = new ArrayList<>();
    }

    public List<HTMLElement> getContent() {
        return content;
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

    public void addTable(List<String> pColumns, Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        Table table = new Table(pColumns);
        ExecutionDataNode<ExecutionData> parent = null;
        for(Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : pClassFileDataMap.entrySet()) {
            if (parent == null) {
                parent = entry.getValue().getParent();
                table.addTableFoot(parent.getData());
            }
            table.addRow(entry.getKey(), entry.getValue().getData());
        }
        content.add(table);
    }

    public void addTable(List<String> pColumns, ClassExecutionData pData) {
        Table table = new Table(pColumns);
        for(Map.Entry<String, List<DefUsePair>> entry: pData.getDefUsePairs().entrySet()){
            int total = entry.getValue().size();
            int covered = pData.computeCoverageForMethod(entry.getKey());
            int missed = total - covered;
            table.addRow(entry.getKey(), total, covered, missed);
        }
        table.addTableFoot(pData);
        content.add(table);
    }
}
