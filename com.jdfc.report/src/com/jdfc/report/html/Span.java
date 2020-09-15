package com.jdfc.report.html;

public class Span extends HTMLElement {

    private final String content;
    private final String id;

    public Span(String pContent, int lineNumber) {
        content = pContent;
        id = "L" + lineNumber;
    }

    @Override
    public String render() {
        String tag = "<span class=\"style-class\" id=\"%s\" >%s</span>";
        return String.format(tag, id, content);
    }
}
