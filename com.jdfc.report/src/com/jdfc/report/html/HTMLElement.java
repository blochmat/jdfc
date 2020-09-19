package com.jdfc.report.html;

import java.util.ArrayList;
import java.util.List;

public class HTMLElement {

    private final String tag;
    private final String text;
    private final List<String> attributes;
    private List<HTMLElement> content;

    private HTMLElement(final String pTag, final String pStyleClass, final String pText){
        tag = pTag;
        text = pText;
        attributes = new ArrayList<>();
        // Add first space for pretty rendering
        attributes.add(" ");
        if (pStyleClass != null) {
            attributes.add(pStyleClass);
        }
    }

    private HTMLElement(final String pTag, String pStyleClass) {
        this(pTag, pStyleClass, null);
        content = new ArrayList<>();
    }

    List<HTMLElement> getContent() {
        return content;
    }

    List<String> getAttributes() {
        return attributes;
    }

    static HTMLElement html(final String pStyleClass){
        String htmlTag = "<!DOCTYPE html><html%s>%s</html>";
        return new HTMLElement(htmlTag, pStyleClass);
    }

    static HTMLElement head(final String pStyleClass){
        String headTag = "<head%s>%s</head>";
        return new HTMLElement(headTag, pStyleClass);
    }

    static HTMLElement body(final String pStyleClass) {
        String bodyTag = "<body%s>%s</body>";
        return new HTMLElement(bodyTag, pStyleClass);
    }

    static HTMLElement link(final String pRel, final String pHref, final String pType){
        String linkTag = "<link%s/>";
        HTMLElement newElement = new HTMLElement(linkTag, null);
        String rel = String.format("rel=\"%s\" ", pRel);
        newElement.attributes.add(rel);
        String href = String.format("href=\"%s\" ", pHref);
        newElement.attributes.add(href);
        String type = String.format("type=\"%s\" ", pType);
        newElement.attributes.add(type);
        return newElement;
    }

    static HTMLElement title(final String pStyleClass, final String pText){
        String titleTag = "<title%s>%s</title>";
        return new HTMLElement(titleTag, pStyleClass, pText);
    }

    static HTMLElement h1(final String pStyleClass, final String pText) {
        String h1Tag = "<h1%s>%s</h1>";
        return new HTMLElement(h1Tag, pStyleClass, pText);
    }

    static HTMLElement div(final String pStyleClass){
        String divTag = "<div%s>%s</div>";
        return new HTMLElement(divTag, pStyleClass);
    }

    static HTMLElement table(final String pStyleClass) {
        String tableTag = "<table%s>%s</table>";
        return new HTMLElement(tableTag, pStyleClass);
    }

    static HTMLElement tbody(final String pStyleClass) {
        String tbodyTag = "<tbody%s>%s</tbody>";
        return new HTMLElement(tbodyTag, pStyleClass);
    }

    static HTMLElement thead(final String pStyleClass) {
        String theadTag = "<thead%s>%s</thead>";
        return new HTMLElement(theadTag, pStyleClass);
    }

    static HTMLElement tfoot(final String pStyleClass) {
        String theadTag = "<tfoot%s>%s</tfoot>";
        return new HTMLElement(theadTag, pStyleClass);
    }

    static HTMLElement tr(final String pStyleClass) {
        String trTag = "<tr%s>%s</tr>";
        return new HTMLElement(trTag, pStyleClass);
    }

    static HTMLElement td(final String pStyleClass) {
        String tdTag = "<td%s>%s</td>";
        return new HTMLElement(tdTag, pStyleClass);
    }

    static HTMLElement td(final String pStyleClass, final String pText) {
        String tdTag = "<td%s>%s</td>";
        return new HTMLElement(tdTag, pStyleClass, pText);
    }

    static HTMLElement td(final String pStyleClass, final int pNumber) {
        String tdTag = "<td%s>%s</td>";
        return new HTMLElement(tdTag, pStyleClass, String.valueOf(pNumber));
    }

    static HTMLElement span(final String pStyleClass) {
        String spanTag = "<span%s>%s</span>";
        return new HTMLElement(spanTag, pStyleClass);
    }

    static HTMLElement span(final String pStyleClass, final String pText) {
        String spanTag = "<span%s>%s</span>";
        return new HTMLElement(spanTag, pStyleClass, pText);
    }

    static HTMLElement pre(final String pStyleClass) {
        String preTag = "<pre%s>%s</pre>";
        return new HTMLElement(preTag, pStyleClass);
    }

    static HTMLElement pre(final String pStyleClass, final String pText) {
        String preTag = "<pre%s>%s</pre>";
        return new HTMLElement(preTag, pStyleClass, pText);
    }

    static HTMLElement noTag(final String pText) {
        return new HTMLElement(null, null, pText);
    }

    static HTMLElement a(final String pStyleClass,
                                final String pLink,
                                final String pText){
        String aTag = "<a%s>%s</a>";
        HTMLElement newElement = new HTMLElement(aTag, pStyleClass, pText);
        String href = String.format("href=\"%s\" ", pLink);
        newElement.attributes.add(href);
        return newElement;
    }

    public String render(){
        if (text == null) {
            return String.format(tag, renderAttributes(), renderContent());
        } else if (tag == null){
            return text;
        } else {
            return String.format(tag, renderAttributes(), text);
        }
    }

    private String renderAttributes(){
        StringBuilder builder = new StringBuilder();
        // No attributes were added
        if (attributes.size() == 1) {
            return "";
        }

        for(String attribute : attributes) {
            builder.append(attribute);
        }
        return builder.toString();
    }

    private String renderContent() {
        StringBuilder builder = new StringBuilder();
        for(HTMLElement element : content) {
            builder.append(element.render());
        }
        return builder.toString();
    }
}
