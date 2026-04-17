package de.metathesis.structures;

import de.metanome.algorithms.hyucc.structures.UCCTreeElementUCCPair;
import de.metathesis.utils.Utility;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * A node in the positive cover prefix tree.
 * Tree structure: each node represents one attribute on the path from root.
 * A path root -> A -> B represents the LHS {A, B}.
 */
@Getter
public class UCCTreeNode {

    @Setter
    private UCCTreeNode[] children;
    private boolean isCandidateUCC;
    private boolean isValidatedUCC;
    private final int numAttributes;

    public UCCTreeNode(int numAttributes) {
        this.numAttributes = numAttributes;
    }

    public UCCTreeNode(int numAttributes, boolean isCandidateUCC) {
        this.numAttributes = numAttributes;
        this.isCandidateUCC = isCandidateUCC;
    }

    public Set<BitSet> getLevel(int level) {
        Set<BitSet> result = new HashSet<>();
        this.getLevelRecursive(level, 0, new BitSet(), result);
        return result;
    }

    private void getLevelRecursive(int level, int currentLevel, BitSet currentUCC, Set<BitSet> result) {
        if (level == currentLevel) {
            if(this.isCandidateUCC && !this.isValidatedUCC){
                result.add((BitSet) currentUCC.clone());
            }
            return;
        }

        if(this.children != null){
            for (int child = 0; child < this.numAttributes; child++) {
                if (this.children[child] == null) {
                    continue;
                }

                currentUCC.set(child);
                this.children[child].getLevelRecursive(level, currentLevel + 1, currentUCC, result);
                currentUCC.clear(child);
            }
        }

        // Cover all paths the tree doesn't have at all
        if (currentLevel < level && this.isCandidateUCC && !this.isValidatedUCC) {
            int additionalDepth = level - currentLevel;
            Set<BitSet>  specializedCandidates = specializeNode(currentUCC, additionalDepth);
            result.addAll(specializedCandidates);
        }
    }

    private Set<BitSet> specializeNode(BitSet currentUCC, int additionalDepth) {
        Set<BitSet>  result = new HashSet<>();

        // Remaining attributes = all - currentLhs
        BitSet remaining = new BitSet(numAttributes);
        remaining.set(0, numAttributes);
        remaining.andNot(currentUCC);

        BitSet[] combinations = Utility.generateApriori(remaining, additionalDepth);

        for (BitSet combo : combinations) {
            BitSet newUCC = (BitSet) currentUCC.clone();
            newUCC.or(combo);

            result.add(newUCC);
        }

        return result;
    }

    // Marks lhs -> rhs as confirmed valid. Sets the bit in rhsValidatedFds at the node for lhs.
    public void markAsValidate(BitSet ucc) {
        UCCTreeNode current = this;
        for (int attr = ucc.nextSetBit(0); attr >= 0; attr = ucc.nextSetBit(attr + 1)) {
            if (current.children == null) {
                current.children = new UCCTreeNode[this.numAttributes];
            }

            if (current.children[attr] == null) {
                current.children[attr] = new UCCTreeNode(numAttributes);
            }

            current = current.children[attr];
        }

        current.isCandidateUCC = true;
        current.isValidatedUCC = true;
    }

    public List<BitSet> getUCCAndGeneralizations(BitSet ucc) {
        List<BitSet> foundUCCs = new ArrayList<>();
        BitSet currentUCC = new BitSet();
        int nextUCCAttr = ucc.nextSetBit(0);
        this.getUCCAndGeneralizationsRecursive(ucc, nextUCCAttr, currentUCC, foundUCCs);
        return foundUCCs;
    }

    private void getUCCAndGeneralizationsRecursive(BitSet ucc, int currentUCCAttr, BitSet currentUCC, List<BitSet> foundUCCs) {
        if (this.isCandidateUCC) {
            foundUCCs.add((BitSet) currentUCC.clone());
        }

        if (this.children == null) {
            return;
        }

        while (currentUCCAttr >= 0) {
            int nextLhsAttr = ucc.nextSetBit(currentUCCAttr + 1);

            if (this.children[currentUCCAttr] != null) {
                currentUCC.set(currentUCCAttr);
                this.children[currentUCCAttr].getUCCAndGeneralizationsRecursive(ucc, nextLhsAttr, currentUCC, foundUCCs);
                currentUCC.clear(currentUCCAttr);
            }

            currentUCCAttr = nextLhsAttr;
        }
    }

    public void addUniqueColumnCombination(BitSet ucc) {
        UCCTreeNode currentNode = this;

        for (int i = ucc.nextSetBit(0); i >= 0; i = ucc.nextSetBit(i + 1)) {
            if (currentNode.children == null) {
                currentNode.children = new UCCTreeNode[this.numAttributes];
                currentNode.children[i] = new UCCTreeNode(this.numAttributes, false);
            } else if (currentNode.children[i] == null) {
                currentNode.children[i] = new UCCTreeNode(this.numAttributes, false);
            }

            currentNode = currentNode.children[i];
        }

        currentNode.isCandidateUCC = true;
    }

    public void removeUniqueColumnCombination(BitSet ucc) {
        int currentUCCAttr = ucc.nextSetBit(0);
        this.removeUniqueColumnCombinationRecursive(ucc, currentUCCAttr);
    }

    private void removeUniqueColumnCombinationRecursive(BitSet ucc, int currentUCCAttr) {
        if (currentUCCAttr < 0) {
            this.isCandidateUCC = false;
            return;
        }

        if ((this.children != null) && (this.children[currentUCCAttr] != null)) {
            this.children[currentUCCAttr].removeUniqueColumnCombinationRecursive(ucc, ucc.nextSetBit(currentUCCAttr + 1));

            if (this.children[currentUCCAttr].isObsolete())
                this.children[currentUCCAttr] = null;
        }
    }

    public boolean containsUCCOrGeneralization(BitSet ucc) {
        int nextUCCAttr = ucc.nextSetBit(0);
        return this.containsUCCOrGeneralizationRecursive(ucc, nextUCCAttr);
    }

    private boolean containsUCCOrGeneralizationRecursive(BitSet ucc, int currentUCCAttr) {
        if (this.isCandidateUCC) {
            return true;
        }

        if (currentUCCAttr < 0) {
            return false;
        }

        int nextUCCAttr = ucc.nextSetBit(currentUCCAttr + 1);

        if ((this.children != null) && (this.children[currentUCCAttr] != null))
            if (this.children[currentUCCAttr].containsUCCOrGeneralizationRecursive(ucc, nextUCCAttr))
                return true;

        return this.containsUCCOrGeneralizationRecursive(ucc, nextUCCAttr);
    }

    private boolean isObsolete() {
        return (!this.hasChildren()) && (!this.isCandidateUCC);
    }

    private boolean hasChildren() {
        if (this.children == null) {
            return false;
        }

        for (UCCTreeNode child : this.children) {
            if (child != null) {
                return true;
            }
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
        sb.append(inner).append("\"cand\": ").append(isCandidateUCC).append(",\n");
        sb.append(inner).append("\"valid\": ").append(isValidatedUCC).append(",\n");
        sb.append(inner).append("\"children\": [");

        List<Integer> childAttrs = new ArrayList<>();
        if (children != null) {
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
}