package data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(ExecutionDataNode.class);
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

    public boolean containsLeafs() {
        for(ExecutionDataNode<T> exData : children.values()) {
            if (exData.isLeaf()) {
                return true;
            }
        }
        return false;
    }

    public void computeClassCoverage() {
        for(ExecutionDataNode<T> node : this.children.values()) {
            ExecutionData data = node.getData();
            if (data instanceof ClassExecutionData) {
                ClassExecutionData cData = (ClassExecutionData) data;
                cData.computeCoverage();
            } else {
                node.computeClassCoverage();
            }
        }
    }

    /**
     * Calculation of coverage values from all children to root node.
     *
     */
    public void aggregateDataToRootRecursive() {
        if (this.isRoot()) {
            this.getData().setMethodCount(this.aggregateMethodCount());
            this.getData().setTotal(this.aggregateTotal());
            this.getData().setCovered(this.aggregateCovered());
            this.getData().setRate(this.aggregateRate());
        }
    }

    private int aggregateMethodCount() {
        if (this.getData() instanceof ClassExecutionData) {
            return this.getData().getMethodCount();
        }

        int mCount = 0;
        for(ExecutionDataNode<T> node : this.getChildren().values()) {
            mCount += node.aggregateMethodCount();
        }

        this.getData().setMethodCount(mCount);
        return mCount;
    }

    private int aggregateTotal() {
        if (this.getData() instanceof ClassExecutionData) {
            return this.getData().getTotal();
        }

        int total = 0;
        for(ExecutionDataNode<T> node : this.getChildren().values()) {
            total += node.aggregateTotal();
        }

        this.getData().setTotal(total);
        return total;
    }

    private int aggregateCovered() {
        if (this.getData() instanceof ClassExecutionData) {
            return this.getData().getCovered();
        }

        int covered = 0;
        for(ExecutionDataNode<T> node : this.getChildren().values()) {
            covered += node.aggregateCovered();
        }
        this.getData().setCovered(covered);
        return covered;
    }

    private double aggregateRate() {
        if (this.getData() instanceof ClassExecutionData) {
            return this.getData().getRate();
        }

        double rate = 0.0;
        if (this.getData().getTotal() != 0) {
            rate = (double) this.getData().getCovered() / this.getData().getTotal();
        }
        this.getData().setRate(rate);
        return rate;
    }
}