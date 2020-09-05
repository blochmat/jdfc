package com.jdfc.report.html;

public class HTMLElement {

    private String tag;
    private String styleClass;
    private String content;

    public HTMLElement (){}

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String render() {
        return String.format(tag, content);
    }
}
