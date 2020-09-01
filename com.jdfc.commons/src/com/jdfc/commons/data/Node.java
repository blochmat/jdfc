package com.jdfc.commons.data;

import java.util.*;

public class Node<T extends NodeData> {

    // Map of children with name, in case of leaf children.size == 0
    private Map<String, Node<T>> children = new HashMap<>();

    private Node<T> parent = null;

    // TODO: Some kind of coverage data
    private T data = null;

    public Node(T data) {
        this.data = data;
    }

    public Node(T data, Node<T> parent) {
        this.data = data;
        this.parent = parent;
    }

    public Map<String, Node<T>> getChildren() {
        return children;
    }

    public Node<T> getChildDataRecursive(ArrayList<String> path){
        if (path.size() == 1){
            return this.getChildren().get(path.get(0));
        } else {
            Node<T> child = this.getChildren().get(path.get(0));
            path.remove(0);
            return child.getChildDataRecursive(path);
        }
    }

    public void setParent(Node<T> parent) {
        this.parent = parent;
    }

    public void addChild(String key, T data) {
        Node<T> child = new Node<T>(data);
        child.setParent(this);
        this.children.put(key, child);
    }

    public void addChild(String key, Node<T> child) {
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
