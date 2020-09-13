package com.jdfc.report.html;

public class Span extends HTMLElement{

    private final String content;

    public Span(String pContent){
        content = pContent;
    }

    @Override
    public String render() {
        String tag = "<span>%s</span>";
        return String.format(tag, content);
    }
}
