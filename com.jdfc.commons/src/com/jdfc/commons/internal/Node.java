package com.jdfc.commons.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node<T> {

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
}
