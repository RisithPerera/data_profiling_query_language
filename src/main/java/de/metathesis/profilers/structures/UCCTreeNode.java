package de.metathesis.profilers.structures;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A node in the positive cover prefix tree.
 * Tree structure: each node represents one attribute on the path from root.
 * A path root -> A -> B represents the LHS {A, B}.
 */
@Getter
public class UCCTreeNode {
    private static final Logger log = LogManager.getLogger(UCCTreeNode.class);

    @Setter
    private UCCTreeNode[] children;
    private boolean isCandidateUCC;
    private boolean isValidatedUCC;
    private final int numAttributes;
    private final ReentrantReadWriteLock access;

    public UCCTreeNode(int numAttributes) {
        this(numAttributes, false, new ReentrantReadWriteLock());
    }

    public UCCTreeNode(int numAttributes, boolean isCandidateUCC, ReentrantReadWriteLock access) {
        this.numAttributes = numAttributes;
        this.isCandidateUCC = isCandidateUCC;
        this.access = access;
    }

    public void init(){
        this.setChildren(new UCCTreeNode[numAttributes]);

        //Initialize Most General Uniques
        for (int attr = 0; attr < this.numAttributes; attr++) {
            this.getChildren()[attr] = new UCCTreeNode(this.numAttributes, true, access);
        }
    }

    public void specializePositiveCover(BitSet nonUCC) {
        access.writeLock().lock();
        log.debug("SpecializePositiveCover GET writeLock");
        try {
            List<BitSet> generalLhsList = this.getUCCAndGeneralizations(nonUCC);

            for (BitSet generalLhs : generalLhsList) {
                this.removeUniqueColumnCombination(generalLhs);

                for (int attr = this.numAttributes - 1; attr >= 0; attr--) {
                    if (!nonUCC.get(attr)) {
                        generalLhs.set(attr);
                        if (!this.containsUCCOrGeneralization(generalLhs)) {
                            this.addUniqueColumnCombination(generalLhs);
                        }
                        generalLhs.clear(attr);
                    }
                }
            }
        } finally {
            access.writeLock().unlock();
            log.debug("SpecializePositiveCover RELEASE writeLock");
        }
    }

    public void markAsValidate(BitSet ucc) {
        access.writeLock().lock();
        log.debug("MarkAsValidate GET writeLock");
        try {
            UCCTreeNode current = this;
            for (int attr = ucc.nextSetBit(0); attr >= 0; attr = ucc.nextSetBit(attr + 1)) {
                if (current.children == null || current.children[attr] == null) {
                    return;
                }

                current = current.children[attr];
            }

            current.isCandidateUCC = true;
            current.isValidatedUCC = true;
        } finally {
            access.writeLock().unlock();
            log.debug("MarkAsValidate RELEASE writeLock");
        }
    }

    public void getValidatedUCCsAtDepth(int targetDepth, Set<BitSet> results) {
        access.readLock().lock();
        log.debug("GetValidatedUCCsAtDepth GET readLock");
        try {
            getValidatedUCCsAtDepthRecursive(this, new BitSet(numAttributes), 0, targetDepth, results);
        } finally {
            access.readLock().unlock();
            log.debug("GetValidatedUCCsAtDepth RELEASE readLock");
        }
    }

    private void getValidatedUCCsAtDepthRecursive(UCCTreeNode node, BitSet currentUCC, int currentDepth, int targetDepth, Set<BitSet> results) {

        if (currentDepth == targetDepth) {
            if (node.isValidatedUCC) {
                results.add((BitSet) currentUCC.clone());
            }
            return;
        }

        if (node.children == null) return;

        for (int i = 0; i < node.numAttributes; i++) {
            if (node.children[i] != null) {
                currentUCC.set(i);
                getValidatedUCCsAtDepthRecursive(node.children[i], currentUCC, currentDepth + 1, targetDepth, results);
                currentUCC.clear(i);
            }
        }
    }

    public Set<BitSet> getLhsPathsAtDepth(int targetDepth) {
        access.readLock().lock();
        log.debug("GetLhsPathsAtDepth GET readLock");
        try {
            Set<BitSet> candidates = new ObjectOpenHashSet<>();
            getLhsPathsAtDepthRecursive(this, new BitSet(numAttributes), 0, targetDepth, candidates);
            return candidates;
        } finally {
            access.readLock().unlock();
            log.debug("GetLhsPathsAtDepth RELEASE readLock");
        }
    }

    private void getLhsPathsAtDepthRecursive(UCCTreeNode node, BitSet currentUCC, int currentDepth, int targetDepth, Set<BitSet> candidates) {

        if (!node.isCandidateUCC && (node.children == null)) return;

        if (node.isCandidateUCC && !node.isValidatedUCC && !currentUCC.isEmpty()) {
            candidates.add((BitSet) currentUCC.clone());
        }

        if (currentDepth == targetDepth) return;

        if (node.children == null) return;

        for (int i = 0; i < node.numAttributes; i++) {
            if (node.children[i] != null) {
                currentUCC.set(i);
                getLhsPathsAtDepthRecursive(node.children[i], currentUCC, currentDepth + 1, targetDepth, candidates);
                currentUCC.clear(i);
            }
        }
    }

    public int getStatusForLhsPath(BitSet lhs) {
        access.readLock().lock();
        log.debug("GetStatusForLhsPath GET readLock");
        try {
            List<BitSet> generalizations = getUCCAndGeneralizations(lhs);

            if (generalizations.isEmpty()){
                return -1; // no UCC exists for this lhs or any generalization
            }

            for (BitSet genLhs : generalizations) {
                if (isValidatedAt(genLhs)){
                    return 1; // validated
                }
            }

            return 0; // found as candidate but not validated
        } finally {
            access.readLock().unlock();
            log.debug("GetStatusForLhsPath RELEASE readLock");
        }
    }

    private boolean isValidatedAt(BitSet lhs) {
        UCCTreeNode node = this;
        for (int attr = lhs.nextSetBit(0); attr >= 0; attr = lhs.nextSetBit(attr + 1)) {
            if (node.isValidatedUCC){
                return true;
            }
            if (node.children == null || node.children[attr] == null){
                return false;
            }
            node = node.children[attr];
        }

        return node.isValidatedUCC;
    }

    private List<BitSet> getUCCAndGeneralizations(BitSet ucc) {
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

    private void addUniqueColumnCombination(BitSet ucc) {
        UCCTreeNode currentNode = this;

        for (int i = ucc.nextSetBit(0); i >= 0; i = ucc.nextSetBit(i + 1)) {
            if (currentNode.children == null) {
                currentNode.children = new UCCTreeNode[this.numAttributes];
                currentNode.children[i] = new UCCTreeNode(this.numAttributes, false, access);
            } else if (currentNode.children[i] == null) {
                currentNode.children[i] = new UCCTreeNode(this.numAttributes, false, access);
            }

            currentNode = currentNode.children[i];
        }

        currentNode.isCandidateUCC = true;
    }

    private void removeUniqueColumnCombination(BitSet ucc) {
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

            // Remove child if it has no children and is not a candidate
            if (!this.children[currentUCCAttr].isCandidateUCC && !this.children[currentUCCAttr].hasChildren()) {
                this.children[currentUCCAttr] = null;
            }
        }
    }

    private boolean containsUCCOrGeneralization(BitSet ucc) {
        int nextUCCAttr = ucc.nextSetBit(0);
        return this.containsUCCOrGeneralizationRecursive(ucc, nextUCCAttr);
    }

    private boolean containsUCCOrGeneralizationRecursive(BitSet ucc, int currentUCCAttr) {
        if (this.isValidatedUCC || this.isCandidateUCC) {
            return true;
        }

        if (currentUCCAttr < 0) {
            return false;
        }

        int nextUCCAttr = ucc.nextSetBit(currentUCCAttr + 1);

        if ((this.children != null) && (this.children[currentUCCAttr] != null)) {
            if (this.children[currentUCCAttr].containsUCCOrGeneralizationRecursive(ucc, nextUCCAttr)) {
                return true;
            }
        }

        return this.containsUCCOrGeneralizationRecursive(ucc, nextUCCAttr);
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