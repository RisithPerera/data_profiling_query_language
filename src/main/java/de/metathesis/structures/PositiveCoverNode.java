package de.metathesis.structures;


import de.metathesis.Utility;
import lombok.Getter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * A node in the positive cover prefix tree.
 *
 * Tree structure: each node represents one attribute on the path from root.
 * A path root -> A -> B represents the LHS {A, B}.
 *
 * rhsAttributes    — propagation marker. If any descendant (or this node)
 *                    has attribute C as a candidate, all ancestors also have
 *                    C set here. Used for fast subtree pruning: if
 *                    rhsAttributes does not contain C, no need to recurse.
 *
 * rhsCandidateFds  — actual FD candidates at exactly this node.
 *                    If bit C is set at the node reached by path {A,B},
 *                    then {A,B} -> C is a current candidate.
 *
 * rhsValidatedFds  — RHS attributes confirmed valid by a completed
 *                    validation pass at this node's level. Used to prune
 *                    specializations: if {A,B} -> C is here, skip {A,B,X} -> C
 *                    when generating level-3 candidates.
 */
@Getter
public class PositiveCoverNode {

    private PositiveCoverNode[] children;
    private final BitSet rhsAttributes;      // propagation marker (union of subtree candidates)
    private final BitSet rhsCandidateFds;    // candidates at exactly this node
    private final BitSet rhsValidatedFds;    // confirmed-valid FDs at this node
    private final int numAttributes;

    public PositiveCoverNode(int numAttributes) {
        this.numAttributes = numAttributes;
        this.rhsAttributes = new BitSet(numAttributes);
        this.rhsCandidateFds = new BitSet(numAttributes);
        this.rhsValidatedFds = new BitSet(numAttributes);
    }

    /**
     * Returns true if this node has no candidates anywhere in its subtree.
     */
    public boolean isEmpty() {
        return rhsAttributes.isEmpty();
    }

    /**
     * After removing or adding candidates at this node, call this on every
     * ancestor to keep rhsAttributes consistent. Recomputes rhsAttributes
     * as the union of rhsCandidateFds and all children's rhsAttributes.
     */
    public void updateRhsAttributes() {
        rhsAttributes.clear();
        rhsAttributes.or(rhsCandidateFds);

        if(children == null) return;

        for (PositiveCoverNode child : children) {
            if (child != null) {
                rhsAttributes.or(child.rhsAttributes);
            }
        }
    }

    public void addFunctionalDependency(BitSet lhs, int rhs) {
        PositiveCoverNode currentNode = this;
        currentNode.rhsAttributes.set(rhs);

        for (int i = lhs.nextSetBit(0); i >= 0; i = lhs.nextSetBit(i + 1)) {
            if (currentNode.children == null) {
                currentNode.children = new PositiveCoverNode[this.numAttributes];
                currentNode.children[i] = new PositiveCoverNode(this.numAttributes);
            } else if (currentNode.children[i] == null) {
                currentNode.children[i] = new PositiveCoverNode(this.numAttributes);
            }

            currentNode = currentNode.children[i];
            currentNode.rhsAttributes.set(rhs);
        }
        currentNode.rhsCandidateFds.set(rhs);
    }

    // Marks lhs -> rhs as confirmed valid. Sets the bit in rhsValidatedFds at the node for lhs.
    public void markAsValidate(BitSet lhs, BitSet rhs) {
        PositiveCoverNode current = this;
        for (int attr = lhs.nextSetBit(0); attr >= 0; attr = lhs.nextSetBit(attr + 1)) {
            if (current.children == null) {
                current.children = new PositiveCoverNode[this.numAttributes];
            }

            if (current.children[attr] == null) {
                current.children[attr] = new PositiveCoverNode(numAttributes);
            }

            current = current.children[attr];
        }

        current.getRhsValidatedFds().or(rhs);
        current.getRhsCandidateFds().or(rhs);
    }

    public List<BitSet> getFdAndGeneralizations(BitSet lhs, int rhs) {
        List<BitSet> foundLhs = new ArrayList<>();
        BitSet currentLhs = new BitSet(); //Root Node
        int nextLhsAttr = lhs.nextSetBit(0);
        this.getFdAndGeneralizationsRecursive(lhs, rhs, nextLhsAttr, currentLhs, foundLhs);
        return foundLhs;
    }

    private void getFdAndGeneralizationsRecursive(BitSet lhs, int rhs, int currentLhsAttr, BitSet currentLhs, List<BitSet> foundLhs) {
        if (this.rhsCandidateFds.get(rhs)) {
            foundLhs.add((BitSet) currentLhs.clone());
        }

        if (this.children == null) {
            return;
        }

        while (currentLhsAttr >= 0) {
            int nextLhsAttr = lhs.nextSetBit(currentLhsAttr + 1);

            if ((this.children[currentLhsAttr] != null) && (this.children[currentLhsAttr].rhsAttributes.get(rhs))) {
                currentLhs.set(currentLhsAttr);
                this.children[currentLhsAttr].getFdAndGeneralizationsRecursive(lhs, rhs, nextLhsAttr, currentLhs, foundLhs);
                currentLhs.clear(currentLhsAttr);
            }

            currentLhsAttr = nextLhsAttr;
        }
    }

    public void removeFunctionalDependency(BitSet lhs, int rhs) {
        int currentLhsAttr = lhs.nextSetBit(0);
        this.removeFunctionalDependencyRecursive(lhs, rhs, currentLhsAttr);
    }

    private boolean removeFunctionalDependencyRecursive(BitSet lhs, int rhs, int currentLhsAttr) {
        // If this is the last attribute of lhs, remove the fd-mark from the rhs
        if (currentLhsAttr < 0) {
            this.rhsCandidateFds.clear(rhs);
            this.rhsAttributes.clear(rhs);
            return true;
        }

        if ((this.children != null) && (this.children[currentLhsAttr] != null)) {
            // Move to the next child with the next lhs attribute
            if (!this.children[currentLhsAttr].removeFunctionalDependencyRecursive(lhs, rhs, lhs.nextSetBit(currentLhsAttr + 1))) {
                return false; // This is a shortcut: if the child was unable to remove the rhs, then this node can also not remove it
            }

            // Delete the child node if it has no rhs attributes any more
            if (this.children[currentLhsAttr].getRhsAttributes().cardinality() == 0) {
                this.children[currentLhsAttr] = null;
            }
        }

        // Check if another child requires the rhs and if not, remove it from this node
        if (this.isLastNodeOf(rhs)) {
            this.rhsAttributes.clear(rhs);
            return true;
        }

        return false;
    }

    private boolean isLastNodeOf(int rhs) {
        if (this.children == null) {
            return true;
        }

        for (PositiveCoverNode child : this.children) {
            if ((child != null) && child.getRhsAttributes().get(rhs)) {
                return false;
            }
        }
        return true;
    }

    public boolean containsFdOrGeneralization(BitSet lhs, int rhs) {
        int nextLhsAttr = lhs.nextSetBit(0);
        return this.containsFdOrGeneralizationRecursive(lhs, rhs, nextLhsAttr);
    }

    private boolean containsFdOrGeneralizationRecursive(BitSet lhs, int rhs, int currentLhsAttr) {
        if (this.rhsValidatedFds.get(rhs)) {
            return false;
        }

        if (this.rhsCandidateFds.get(rhs)) {
            return true;
        }

        // Is the dependency already read and we have not yet found a generalization?
        if (currentLhsAttr < 0)
            return false;

        int nextLhsAttr = lhs.nextSetBit(currentLhsAttr + 1);

        if ((this.children != null) && (this.children[currentLhsAttr] != null) && (this.children[currentLhsAttr].rhsAttributes.get(rhs))) {
            if (this.children[currentLhsAttr].containsFdOrGeneralizationRecursive(lhs, rhs, nextLhsAttr)) {
                return true;
            }
        }

        return this.containsFdOrGeneralizationRecursive(lhs, rhs, nextLhsAttr);
    }

    public boolean hasValidatedGeneralization(BitSet lhs, int rhs) {
        return hasValidatedGeneralizationRecursive(this, lhs, new BitSet(), rhs);
    }

    private boolean hasValidatedGeneralizationRecursive(PositiveCoverNode node, BitSet targetLhs, BitSet currentLhs, int rhs) {

        if (node == null) return false;

        // Fast prune — if rhs not anywhere in this subtree, skip
        if (!node.rhsAttributes.get(rhs)) return false;

        // Check if this node has rhs validated and currentLhs ⊆ targetLhs
        if (node.rhsValidatedFds.get(rhs) && Utility.isSubset(currentLhs, targetLhs)) {
            return true;
        }

        // Only recurse into children whose attribute is in targetLhs
        for (int attr = targetLhs.nextSetBit(0); attr >= 0; attr = targetLhs.nextSetBit(attr + 1)) {
            if (currentLhs.get(attr)) continue;

            if (node.children == null || node.children[attr] == null) continue;

            currentLhs.set(attr);
            if (hasValidatedGeneralizationRecursive(node.children[attr], targetLhs, currentLhs, rhs)) {
                currentLhs.clear(attr);
                return true;
            }
            currentLhs.clear(attr);
        }

        return false;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        buildJson(sb, -1, 0);
        return sb.toString();
    }

    private void buildJson(StringBuilder sb, int nodeAttr, int depth) {
        String indent = "  ".repeat(depth);
        String inner  = "  ".repeat(depth + 1);

        sb.append(indent).append("{\n");
        sb.append(inner).append("\"node\": ").append(nodeAttr == -1 ? "\"root\"" : nodeAttr).append(",\n");
        sb.append(inner).append("\"cand\": ").append(bitSetToJsonArray(rhsCandidateFds)).append(",\n");
        sb.append(inner).append("\"valid\": ").append(bitSetToJsonArray(rhsValidatedFds)).append(",\n");
        sb.append(inner).append("\"prop\": ").append(bitSetToJsonArray(rhsAttributes)).append(",\n");
        sb.append(inner).append("\"children\": [");

        // Collect non-null children first
        List<Integer> childAttrs = new ArrayList<>();
        if(children != null) {
            for (int attr = 0; attr < numAttributes; attr++) {
                if (children[attr] != null) childAttrs.add(attr);
            }
        }

        if (childAttrs.isEmpty()) {
            sb.append("]\n");
        } else {
            sb.append("\n");
            for (int i = 0; i < childAttrs.size(); i++) {
                int attr = childAttrs.get(i);
                children[attr].buildJson(sb, attr, depth + 2);
                if (i < childAttrs.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(inner).append("]\n");
        }

        sb.append(indent).append("}");
    }

    private String bitSetToJsonArray(BitSet bs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            sb.append(i).append(",");
        }
        if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }
}