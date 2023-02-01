package report.html;

import java.util.ArrayList;
import java.util.List;

public class HTMLElement {

    private final String tag;
    private final String text;
    private final List<String> attributes;
    private List<HTMLElement> content;

    private HTMLElement(final String pTag, final String pText) {
        tag = pTag;
        text = pText;
        attributes = new ArrayList<>();
    }

    private HTMLElement(final String pTag) {
        this(pTag, null);
        content = new ArrayList<>();
    }

    List<HTMLElement> getContent() {
        return content;
    }

    List<String> getAttributes() {
        return attributes;
    }

    static HTMLElement html() {
        String htmlTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\"%s>%s</html>";
        return new HTMLElement(htmlTag);
    }

    static HTMLElement head() {
        String headTag = "<head%s>%s</head>";
        return new HTMLElement(headTag);
    }

    static HTMLElement body() {
        String bodyTag = "<body%s>%s</body>";
        return new HTMLElement(bodyTag);
    }

    static HTMLElement link(final String pRel, final String pHref, final String pType) {
        String linkTag = "<link%s/>";
        HTMLElement newElement = new HTMLElement(linkTag, null);
        String rel = String.format("rel=\"%s\"", pRel);
        newElement.attributes.add(rel);
        String href = String.format("href=\"%s\"", pHref);
        newElement.attributes.add(href);
        String type = String.format("type=\"%s\"", pType);
        newElement.attributes.add(type);
        return newElement;
    }

    static HTMLElement script(final String pType, final String pSrc) {
        String scriptTag = "<script%s></script>";
        HTMLElement newElement = new HTMLElement(scriptTag, null);
        String type = String.format("type=\"%s\"", pType);
        newElement.getAttributes().add(type);
        String src = String.format("src=\"%s\"", pSrc);
        newElement.getAttributes().add(src);
        return newElement;
    }

    static HTMLElement title(final String pText) {
        String titleTag = "<title%s>%s</title>";
        return new HTMLElement(titleTag, pText);
    }

    static HTMLElement h1(final String pText) {
        String h1Tag = "<h1%s>%s</h1>";
        return new HTMLElement(h1Tag, pText);
    }

    static HTMLElement div() {
        String divTag = "<div%s>%s</div>";
        return new HTMLElement(divTag);
    }

    static HTMLElement table() {
        String tableTag = "<table%s>%s</table>";
        return new HTMLElement(tableTag);
    }

    static HTMLElement tbody() {
        String tbodyTag = "<tbody%s>%s</tbody>";
        return new HTMLElement(tbodyTag);
    }

    static HTMLElement thead() {
        String theadTag = "<thead%s>%s</thead>";
        return new HTMLElement(theadTag);
    }

    static HTMLElement tfoot() {
        String theadTag = "<tfoot%s>%s</tfoot>";
        return new HTMLElement(theadTag);
    }

    static HTMLElement tr() {
        String trTag = "<tr%s>%s</tr>";
        return new HTMLElement(trTag);
    }

    static HTMLElement td() {
        String tdTag = "<td%s>%s</td>";
        return new HTMLElement(tdTag);
    }

    static HTMLElement td(final String pText) {
        String tdTag = "<td%s>%s</td>";
        return new HTMLElement(tdTag, pText);
    }

    static HTMLElement td(final int pNumber) {
        String tdTag = "<td%s>%s</td>";
        return new HTMLElement(tdTag, String.valueOf(pNumber));
    }

    static HTMLElement span(final String pText) {
        String spanTag = "<span%s>%s</span>";
        return new HTMLElement(spanTag, pText);
    }

    static HTMLElement pre() {
        String preTag = "<pre%s>%s</pre>";
        return new HTMLElement(preTag);
    }

    static HTMLElement pre(final String pText) {
        String preTag = "<pre%s>%s</pre>";
        return new HTMLElement(preTag, pText);
    }

    static HTMLElement button(final String pText) {
        String buttonTag = "<button%s>%s</button>";
        return new HTMLElement(buttonTag, pText);
    }

    static HTMLElement noTag(final String pText) {
        return new HTMLElement(null, pText);
    }

    static HTMLElement a(final String pLink,
                         final String pText) {
        String aTag = "<a%s>%s</a>";
        HTMLElement newElement = new HTMLElement(aTag, pText);
        String href = String.format("href=\"%s\" ", pLink);
        newElement.attributes.add(href);
        return newElement;
    }

    public String render() {
        if(tag == null) {
            return text;
        }

        if (content == null) {
            if(text == null) {
                return String.format(tag, renderAttributes());
            } else {
                return String.format(tag, renderAttributes(), text);
            }
        } else {
            return String.format(tag, renderAttributes(), renderContent());
        }
    }

    private String renderAttributes() {
        if(attributes.isEmpty()){
            return "";
        }
        attributes.add(0, " ");
        return String.join(" ", attributes);
    }

    private String renderContent() {
        StringBuilder builder = new StringBuilder();
        for (HTMLElement element : content) {
            builder.append(element.render());
        }
        return builder.toString();
    }
}
