package com.jdfc.report.html;

import com.jdfc.commons.data.ExecutionData;
import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.cfg.DefUsePair;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.report.html.table.Table;

import java.util.*;

public class HTMLFile {

    private final String title;
    private final String HMTLContainer;
    private String head;
    private String body;
    private List<HTMLElement> contentList;

    public HTMLFile(String title){
        this.title = title;
        this.HMTLContainer = "<!DOCTYPE html><html>%s</br>%s</html>";
        this.head = "<head>%s</head>";
        this.body = "<body>%s</body>";
        contentList = new ArrayList<>();
    }

    public List<HTMLElement> getContentList() {
        return contentList;
    }

    public String render(){
        body = renderBody();
        return String.format(HMTLContainer, head, body);
    }

    private String renderBody() {
        String render = String.format("<h1 class=\"style-class\">%s</h1>", title);
        for(HTMLElement el : contentList){
            render = render.concat(el.render());
        }
        return String.format(body, render);
    }

    public void createHeader(){
        // mainly style and title
        String linkToResource ="<link rel=\"stylesheet\" href=\"../../jdfc-resources/report.css\" type=\"text/css\"/>";
        String title = String.format("<title>%s.java</title>", this.title);
        String content = linkToResource.concat(title);
        head = String.format(head, content);
    }

    public void addTable(Table table){
        contentList.add(table);
    }

    // Package and Class Tables Index
    public void addTable(List<String> pColumns, Map<String, ExecutionDataNode<ExecutionData>> pClassFileDataMap) {
        Table table = new Table(pColumns);
        ExecutionDataNode<ExecutionData> parent = null;
        for(Map.Entry<String, ExecutionDataNode<ExecutionData>> entry : pClassFileDataMap.entrySet()) {
            if (parent == null) {
                parent = entry.getValue().getParent();
                table.createFoot(parent.getData());
            }
            ExecutionData data = entry.getValue().getData();
            String[] rowEntries =
                    {entry.getKey(), String.valueOf(data.getMethodCount()), String.valueOf(data.getTotal()),
                            String.valueOf(data.getCovered()), String.valueOf(data.getMissed())};
            if (data instanceof ClassExecutionData) {
                String link = entry.getKey()+".html";
                table.addRow(rowEntries, link);
            } else {
                table.addRow(rowEntries, entry.getKey());
            }

        }
        contentList.add(table);
    }

    // MethodTable
    public void addTable(List<String> pColumns, ClassExecutionData pData, String classFileName) {
        Table table = new Table(pColumns);
        for(Map.Entry<String, List<DefUsePair>> entry: pData.getDefUsePairs().entrySet()){
            int total = entry.getValue().size();
            int covered = pData.computeCoverageForMethod(entry.getKey());
            int missed = total - covered;
            String[] entries =
                    {entry.getKey(), String.valueOf(total), String.valueOf(covered), String.valueOf(missed)};
            String link = String.format("%s.java.html#L%s", classFileName, pData.getMethodStartLineMap().get(entry.getKey()));
            table.addRow(entries, link);
        }
        table.createFoot(pData);
        contentList.add(table);
    }
}
