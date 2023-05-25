package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Representing one element in the project structure of the project under test.
 * Either a package or class is represented. Every {@code ExecutionDataNode} stores a {@code ExecutionData}
 * instance depending on the element represented.
 *
 * @param <T> Coverage data container
 */
public class ExecutionDataNode<T extends ExecutionData> {

    private final Map<String, ExecutionDataNode<T>> children = new HashMap<>();
    private ExecutionDataNode<T> parent = null;
    private T data;

    public ExecutionDataNode(T data) {
        this.data = data;
    }

    public Map<String, ExecutionDataNode<T>> getChildren() {
        return children;
    }

    public void setParent(ExecutionDataNode<T> parent) {
        this.parent = parent;
    }

    public ExecutionDataNode<T> getParent() {
        return parent;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }


    /**
     * Iterating the data tree structure until the passed path has length = 1.
     * @param path path leading to the required data node
     * @return node found by the path passed or null
     */
    public ExecutionDataNode<T> getChildDataRecursive(ArrayList<String> path) {
        if (path.size() == 1) {
            if(this.getChildren() != null && !this.children.isEmpty()) {
                return this.getChildren().get(path.get(0));
            } else {
                return null;
            }
        } else {
            ExecutionDataNode<T> child = this.getChildren().get(path.get(0));
            path.remove(0);
            if (child != null) {
                return child.getChildDataRecursive(path);
            } else {
                return null;
            }
        }
    }

    /**
     * Adds new child to a node
     *
     * @param key key identifying child
     * @param data data stored by the key
     */
    public void addChild(String key, T data) {
        ExecutionDataNode<T> child = new ExecutionDataNode<T>(data);
        child.setParent(this);
        this.children.put(key, child);
    }

    public void addChild(String key, ExecutionDataNode<T> child) {
        child.setParent(this);
        this.children.put(key, child);
    }

    /**
     *
     * @return true if parent == null
     */
    public boolean isRoot() {
        return (this.parent == null);
    }

    /**
     *
     * @return true if children.size() == 0
     */
    public boolean isLeaf() {
        return this.children.size() == 0;
    }

    /**
     * Calculation of coverage values from all children to root node
     */
    public void aggregateDataToRootRecursive() {
        if (!this.isRoot()) {
            int newTotal = 0;
            int newCovered = 0;
            int newMethodCount = 0;

            for(Map.Entry<String, ExecutionDataNode<T>> child : this.parent.getChildren().entrySet()){
                newTotal += child.getValue().getData().getTotal();
                newCovered += child.getValue().getData().getCovered();
                newMethodCount += child.getValue().getData().getMethodCount();
            }
            ExecutionData parentData = this.parent.getData();
            parentData.setTotal(newTotal);
            parentData.setCovered(newCovered);
            parentData.setMethodCount(newMethodCount);
            this.parent.aggregateDataToRootRecursive();
        }
    }
}
