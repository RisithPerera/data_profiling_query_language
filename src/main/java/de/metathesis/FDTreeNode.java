package de.metathesis;

import lombok.Getter;
import lombok.Setter;

import java.util.BitSet;

public class FDTreeNode {
    @Getter
    @Setter
    public BitSet fds;

    @Getter
    public FDTreeNode[] children;

    public FDTreeNode(int numAttributes) {
        this.fds = new BitSet(numAttributes);
        this.children = null;
    }

    public FDTreeNode getOrCreateChild(int attr, int numAttributes) {
        if (children == null){
            children = new FDTreeNode[numAttributes];
        }

        if (children[attr] == null){
            children[attr] = new FDTreeNode(numAttributes);
        }

        return children[attr];
    }
}
