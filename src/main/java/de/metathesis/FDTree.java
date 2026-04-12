package de.metathesis;

import lombok.Getter;

import java.util.BitSet;

public class FDTree {
    @Getter
    public final FDTreeNode root;

    @Getter
    public final int numAttributes;

    public FDTree(int numAttributes) {
        this.numAttributes = numAttributes;
        this.root = new FDTreeNode(numAttributes);
    }

    public FDTreeNode getOrCreate(BitSet lhs) {
        FDTreeNode node = root;
        for (int attr = lhs.nextSetBit(0); attr >= 0; attr = lhs.nextSetBit(attr + 1)) {
            node = node.getOrCreateChild(attr, numAttributes);
        }
        return node;
    }

    public FDTreeNode get(BitSet lhs) {
        FDTreeNode node = root;
        for (int attr = lhs.nextSetBit(0); attr >= 0; attr = lhs.nextSetBit(attr + 1)) {
            if (node.getChildren() == null){
                return null;
            }

            node = node.getChildren()[attr];
            if (node == null){
                return null;
            }
        }
        return node;
    }
}
