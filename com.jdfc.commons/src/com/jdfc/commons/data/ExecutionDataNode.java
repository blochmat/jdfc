package com.jdfc.commons.data;

import java.util.*;

public class ExecutionDataNode<T extends ExecutionData> {

    // Map of children with name, in case of leaf children.size == 0
    private Map<String, ExecutionDataNode<T>> children = new HashMap<>();

    private ExecutionDataNode<T> parent = null;

    // TODO: Some kind of coverage data
    private T data = null;

    public ExecutionDataNode(T data) {
        this.data = data;
    }

    public ExecutionDataNode(T data, ExecutionDataNode<T> parent) {
        this.data = data;
        this.parent = parent;
    }

    public Map<String, ExecutionDataNode<T>> getChildren() {
        return children;
    }

    public ExecutionDataNode<T> getChildDataRecursive(ArrayList<String> path){
        if (path.size() == 1){
            return this.getChildren().get(path.get(0));
        } else {
            ExecutionDataNode<T> child = this.getChildren().get(path.get(0));
            path.remove(0);
            return child.getChildDataRecursive(path);
        }
    }

    public void setParent(ExecutionDataNode<T> parent) {
        this.parent = parent;
    }

    public void addChild(String key, T data) {
        ExecutionDataNode<T> child = new ExecutionDataNode<T>(data);
        child.setParent(this);
        this.children.put(key, child);
    }

    public void addChild(String key, ExecutionDataNode<T> child) {
        child.setParent(this);
        this.children.put(key, child);
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isRoot() {
        return (this.parent == null);
    }

    public boolean isLeaf() {
        return this.children.size() == 0;
    }

    public void removeParent() {
        this.parent = null;
    }

    public boolean hasChild(String path){
        if (path == null) {
            return false;
        }
        String[] pathArray = path.split("/");
        if (pathArray.length > 1) {
            return hasChild(String.join("/", pathArray)) && children.containsKey(pathArray[0]);
        } else {
            return children.containsKey(pathArray[0]);
        }
    }
}
