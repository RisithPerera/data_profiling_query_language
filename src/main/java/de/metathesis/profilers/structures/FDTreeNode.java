package de.metathesis.profilers.structures;

import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import lombok.Getter;

import java.util.*;

/**
 * A node in the positive cover prefix tree.

 * Tree structure: each node represents one attribute on the path from root.
 * A path root -> A -> B represents the LHS {A, B}.

 * rhsAttributes    — propagation marker. If any descendant (or this node)
 *                    has attribute C as a candidate, all ancestors also have
 *                    C set here. Used for fast subtree pruning: if
 *                    rhsAttributes does not contain C, no need to recurse.

 * rhsCandidateFds  — actual FD candidates at exactly this node.
 *                    If bit C is set at the node reached by path {A,B},
 *                    then {A,B} -> C is a current candidate.

 * rhsValidatedFds  — RHS attributes confirmed valid by a completed
 *                    validation pass at this node's level. Used to prune
 *                    specializations: if {A,B} -> C is here, skip {A,B,X} -> C
 *                    when generating level-3 candidates.
 */
@Getter
public class FDTreeNode {

    private FDTreeNode[] children;
    private final BitSet rhsAttributes;      // propagation marker (union of subtree candidates)
    private final BitSet rhsCandidateFds;    // candidates at exactly this node
    private final BitSet rhsValidatedFds;    // confirmed-valid FDs at this node
    private final int numAttributes;

    public FDTreeNode(int numAttributes) {
        this.numAttributes = numAttributes;
        this.rhsAttributes = new BitSet(numAttributes);
        this.rhsCandidateFds = new BitSet(numAttributes);
        this.rhsValidatedFds = new BitSet(numAttributes);
    }

    public record FDSearchResult(BitSet lhs, BitSet confirmed, BitSet remaining) {}

    public BitSet specializePositiveCover(BitSet lhs, int rhsAttr) {
        List<BitSet> generalLhsList = this.getFdAndGeneralizations(lhs, rhsAttr);
        BitSet specialized = new BitSet();

        for (BitSet generalLhs : generalLhsList) {
            this.removeFunctionalDependency(generalLhs, rhsAttr);

            for (int attr = this.numAttributes - 1; attr >= 0; attr--) { // TODO: Is iterating backwards a good or bad idea?
                if (!lhs.get(attr) && (attr != rhsAttr)) {
                    generalLhs.set(attr);
                    if (!this.containsFdOrGeneralization(generalLhs, rhsAttr)) {
                        this.addFunctionalDependency(generalLhs, rhsAttr);
                        specialized.set(attr);
                    }
                    generalLhs.clear(attr);
                }
            }
        }

        return specialized;
    }

    private synchronized void addFunctionalDependency(BitSet lhs, int rhs) {
        FDTreeNode currentNode = this;
        currentNode.rhsAttributes.set(rhs);

        for (int i = lhs.nextSetBit(0); i >= 0; i = lhs.nextSetBit(i + 1)) {
            if (currentNode.children == null) {
                currentNode.children = new FDTreeNode[this.numAttributes];
                currentNode.children[i] = new FDTreeNode(this.numAttributes);
            } else if (currentNode.children[i] == null) {
                currentNode.children[i] = new FDTreeNode(this.numAttributes);
            }

            currentNode = currentNode.children[i];
            currentNode.rhsAttributes.set(rhs);
        }
        currentNode.rhsCandidateFds.set(rhs);
    }

    // Marks lhs -> rhs as confirmed valid. Sets the bit in rhsValidatedFds at the node for lhs.
    public synchronized void markAsValidate(BitSet lhs, BitSet rhs) {
        FDTreeNode current = this;
        for (int attr = lhs.nextSetBit(0); attr >= 0; attr = lhs.nextSetBit(attr + 1)) {
            if (current.children == null) {
                current.children = new FDTreeNode[this.numAttributes];
            }

            if (current.children[attr] == null) {
                current.children[attr] = new FDTreeNode(numAttributes);
            }

            current = current.children[attr];
        }

        // Only mark bits that are actually owned by this node
        BitSet toMark = (BitSet) rhs.clone();
        toMark.and(current.rhsAttributes);

        current.rhsValidatedFds.or(toMark);
        current.rhsCandidateFds.or(toMark);
    }

    public LinkedHashSet<ObjectObjectImmutablePair<BitSet, Boolean>> getLhsPathsForRhsNew(BitSet targetRhs, int rhsBit) {
        // Collect minimal lhs paths for each target rhs bit
        LinkedHashSet<ObjectObjectImmutablePair<BitSet, Boolean>> paths = new LinkedHashSet<>();
        getLhsPathsForRhsNewRecursive(this, new BitSet(numAttributes), rhsBit, targetRhs, paths);
        return paths;
    }

    private void getLhsPathsForRhsNewRecursive(FDTreeNode node, BitSet currentLhs, int rhsBit, BitSet targetRhs, LinkedHashSet<ObjectObjectImmutablePair<BitSet, Boolean>> result) {
        // this path has no relevance to rhsBit
        if (!node.rhsAttributes.get(rhsBit)) return;

        if (node.rhsCandidateFds.get(rhsBit) || node.rhsValidatedFds.get(rhsBit)) {
            boolean isValidated = node.rhsValidatedFds.get(rhsBit);

            result.add(new ObjectObjectImmutablePair<>((BitSet) currentLhs.clone(), isValidated));
            return;
        }

        if (node.children == null) return;

        for (int i = 0; i < node.children.length; i++) {
            //skip paths containing targetRhs bits because lhs path should not contain given target rhs
            if (!targetRhs.get(i) && node.children[i] != null) {
                currentLhs.set(i);
                getLhsPathsForRhsNewRecursive(node.children[i], currentLhs, rhsBit, targetRhs, result);
                currentLhs.clear(i);
            }
        }
    }

    public Map<Integer, List<FDSearchResult>> getLhsPathsForRhs(BitSet targetRhs) {
        // Collect minimal lhs paths for each target rhs bit
        Map<Integer, List<FDSearchResult>> perBitPaths = new LinkedHashMap<>();
        for (int rhsBit = targetRhs.nextSetBit(0); rhsBit >= 0; rhsBit = targetRhs.nextSetBit(rhsBit + 1)) {
            List<FDSearchResult> paths = new ArrayList<>();
            getLhsPathsForSingleRhs(this, new BitSet(numAttributes), rhsBit, targetRhs, paths);
            if (paths.isEmpty()){
                return new HashMap<>(); //Return early if at least one target rhs doesnt have any Lhs path
            }
            perBitPaths.put(rhsBit, paths);
        }

        return perBitPaths;
    }

    private void getLhsPathsForSingleRhs(FDTreeNode node, BitSet currentLhs, int rhsBit, BitSet targetRhs, List<FDSearchResult> result) {
        // this path has no relevance to rhsBit
        if (!node.rhsAttributes.get(rhsBit)) return;

        if (node.rhsCandidateFds.get(rhsBit) || node.rhsValidatedFds.get(rhsBit)) {
            BitSet confirmed = new BitSet();
            BitSet remaining = new BitSet();
            if (node.rhsValidatedFds.get(rhsBit)){
                confirmed.set(rhsBit);
            } else {
                remaining.set(rhsBit);
            }
            result.add(new FDSearchResult((BitSet) currentLhs.clone(), confirmed, remaining));
            return;
        }

        if (node.children == null) return;

        for (int i = 0; i < node.children.length; i++) {
            //skip paths containing targetRhs bits because lhs path should not contain given target rhs
            if (!targetRhs.get(i) && node.children[i] != null) {
                currentLhs.set(i);
                getLhsPathsForSingleRhs(node.children[i], currentLhs, rhsBit, targetRhs, result);
                currentLhs.clear(i);
            }
        }
    }

    public ObjectObjectImmutablePair<BitSet, BitSet> searchByLhs(BitSet lhs, BitSet rhs) {
        BitSet confirmed = new BitSet(numAttributes);
        BitSet possible = new BitSet(numAttributes);

        for (int rhsBit = rhs.nextSetBit(0); rhsBit >= 0; rhsBit = rhs.nextSetBit(rhsBit + 1)) {

            // Check exact path + all generalizations for this rhs bit
            List<BitSet> generalizations = getFdAndGeneralizations(lhs, rhsBit);

            for(BitSet genLhs : generalizations){
                // Check if any of these are validated or just candidates
                boolean isValidated = isValidatedAt(genLhs, rhsBit);

                if (isValidated) {
                    confirmed.set(rhsBit);
                } else {
                    possible.set(rhsBit);
                }
            }
        }

        return new ObjectObjectImmutablePair<>(confirmed, possible);
    }

    private boolean isValidatedAt(BitSet lhs, int rhs) {
        FDTreeNode node = this;
        for (int attr = lhs.nextSetBit(0); attr >= 0; attr = lhs.nextSetBit(attr + 1)) {
            if (node.children == null || node.children[attr] == null){
                return false;
            }
            node = node.children[attr];
        }
        return node.rhsValidatedFds.get(rhs);
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

    private synchronized void removeFunctionalDependency(BitSet lhs, int rhs) {
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

        for (FDTreeNode child : this.children) {
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
        if (this.rhsCandidateFds.get(rhs) || this.rhsValidatedFds.get(rhs)) {
            return true;
        }

        // Is the dependency already read and we have not yet found a generalization?
        if (currentLhsAttr < 0) {
            return false;
        }

        int nextLhsAttr = lhs.nextSetBit(currentLhsAttr + 1);

        if ((this.children != null) && (this.children[currentLhsAttr] != null) && (this.children[currentLhsAttr].rhsAttributes.get(rhs))) {
            if (this.children[currentLhsAttr].containsFdOrGeneralizationRecursive(lhs, rhs, nextLhsAttr)) {
                return true;
            }
        }

        return this.containsFdOrGeneralizationRecursive(lhs, rhs, nextLhsAttr);
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