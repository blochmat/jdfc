package com.jdfc.report.html.table;

public class Foot extends Row {

    public Foot(String[] entries){
        super(entries);
    }

    @Override
    public String render() {
        String tag = "<tfoot>%s</tfoot>";
        return String.format(tag, super.render());
    }
}
