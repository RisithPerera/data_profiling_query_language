package de.metathesis.structures;

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

    public synchronized void addFunctionalDependency(BitSet lhs, int rhs) {
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

    public record RhsSearchResult(BitSet lhs, BitSet confirmed, BitSet remaining) {}

    public List<RhsSearchResult> findAllLhsForRhs(BitSet targetRhs) {
        // collect minimal lhs paths
        Map<Integer, List<ObjectObjectImmutablePair<BitSet, Boolean>>> perBitPaths = new LinkedHashMap<>();
        for (int bit = targetRhs.nextSetBit(0); bit >= 0; bit = targetRhs.nextSetBit(bit + 1)) {
            List<ObjectObjectImmutablePair<BitSet, Boolean>> paths = new ArrayList<>();
            collectLhsForSingleRhs(this, new BitSet(numAttributes), bit, targetRhs, paths);
            if (paths.isEmpty()) return Collections.emptyList();
            perBitPaths.put(bit, paths);
        }

        // initialize result with first bit
        Iterator<Map.Entry<Integer, List<ObjectObjectImmutablePair<BitSet, Boolean>>>> it = perBitPaths.entrySet().iterator();
        Map.Entry<Integer, List<ObjectObjectImmutablePair<BitSet, Boolean>>> first = it.next();
        int firstBit = first.getKey();

        List<RhsSearchResult> result = new ArrayList<>();
        for (ObjectObjectImmutablePair<BitSet, Boolean> p : first.getValue()) {
            BitSet confirmed = new BitSet(numAttributes);
            BitSet remaining = new BitSet(numAttributes);
            if (p.right()) confirmed.set(firstBit);
            else remaining.set(firstBit);
            result.add(new RhsSearchResult((BitSet) p.left().clone(), confirmed, remaining));
        }

        // iteratively merge with each subsequent bit
        while (it.hasNext()) {
            Map.Entry<Integer, List<ObjectObjectImmutablePair<BitSet, Boolean>>> entry = it.next();
            int bit = entry.getKey();
            List<RhsSearchResult> merged = new ArrayList<>();

            for (RhsSearchResult current : result) {
                for (ObjectObjectImmutablePair<BitSet, Boolean> next : entry.getValue()) {
                    BitSet mergedLhs = (BitSet) current.lhs().clone();
                    mergedLhs.or(next.left());

                    if (isSuperset(mergedLhs, merged)) continue;

                    BitSet mergedConfirmed = (BitSet) current.confirmed().clone();
                    BitSet mergedRemaining = (BitSet) current.remaining().clone();
                    if (next.right()) mergedConfirmed.set(bit);
                    else mergedRemaining.set(bit);

                    merged.add(new RhsSearchResult(mergedLhs, mergedConfirmed, mergedRemaining));
                }
            }

            result = minimize(merged);
        }

        return result;
    }

    private void collectLhsForSingleRhs(FDTreeNode node, BitSet currentLhs, int rhsBit, BitSet targetRhs, List<ObjectObjectImmutablePair<BitSet, Boolean>> result) {
        // this path has no relevance to rhsBit
        if (!node.rhsAttributes.get(rhsBit)) return;

        if (node.rhsCandidateFds.get(rhsBit) || node.rhsValidatedFds.get(rhsBit)) {
            boolean isConfirmed = node.rhsValidatedFds.get(rhsBit);
            result.add(new ObjectObjectImmutablePair<>((BitSet) currentLhs.clone(), isConfirmed));
            return; // Once found rhsBit avoid go further on this path to keep only minimal
        }

        if (node.children == null) return;

        for (int i = 0; i < node.children.length; i++) {
            //skip paths containing targetRhs bits because lhs path should not contain given target rhs
            if (!targetRhs.get(i) && node.children[i] != null) {
                currentLhs.set(i);
                collectLhsForSingleRhs(node.children[i], currentLhs, rhsBit, targetRhs, result);
                currentLhs.clear(i);
            }
        }
    }

    private boolean isSuperset(BitSet lhs, List<RhsSearchResult> list) {
        for (RhsSearchResult r : list) {
            BitSet copy = (BitSet) r.lhs().clone();
            copy.andNot(lhs);
            if (copy.isEmpty()) return true;
        }
        return false;
    }

    private List<RhsSearchResult> minimize(List<RhsSearchResult> list) {
        List<RhsSearchResult> minimal = new ArrayList<>();
        for (RhsSearchResult candidate : list) {
            if (!isSuperset(candidate.lhs(), minimal)) {
                minimal.removeIf(r -> {
                    BitSet copy = (BitSet) candidate.lhs().clone();
                    copy.andNot(r.lhs());
                    return copy.isEmpty();
                });
                minimal.add(candidate);
            }
        }
        return minimal;
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

    public synchronized void removeFunctionalDependency(BitSet lhs, int rhs) {
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