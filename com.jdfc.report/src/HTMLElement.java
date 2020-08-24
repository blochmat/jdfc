import java.io.Writer;

public class HTMLElement {

    private Writer writer;
    private String name;
    private boolean isOpened;
    private boolean isClosed;
    private boolean isRoot;

    public HTMLElement(Writer writer, String name, boolean isRoot){
        this.writer = writer;
        this.name = name;
        isOpened = false;
        isClosed = false;
        this.isRoot = isRoot;
    }
}
