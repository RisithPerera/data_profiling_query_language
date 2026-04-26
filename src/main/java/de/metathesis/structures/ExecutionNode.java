package de.metathesis.structures;

import de.metaserve.executor.min.graph.edge.Edge;
import de.metathesis.profilers.ProfilerFactory;
import de.metathesis.structures.requests.SearchSpace;
import de.metathesis.structures.results.Result;
import de.metathesis.utils.Utility;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Getter
public final class ExecutionNode {
    private static final Logger log = LogManager.getLogger(ExecutionNode.class);

    private final Edge edge;
    private final int level;
    private final ProfilerFactory factory;

    private final Map<String, int[]> relationMap = new HashMap<>();
    private final Map<String, ExecutionNode> parents = new HashMap<>();
    private final List<ExecutionNode> children = new ArrayList<>(); //To Track Leaf Nodes

    private CompletableFuture<? extends Result<?>> future;
    private Result<?> results;

    public ExecutionNode(Edge edge, int level, ProfilerFactory factory) {
        this.edge = edge;
        this.level = level;
        this.factory = factory;
    }

    public void execute() {
        if (Objects.nonNull(this.future)) {
            return; // prevent double execution
        }

        CompletableFuture<?>[] parents = this.parents.values().stream()
                    .map(ExecutionNode::getFuture)
                    .toArray(CompletableFuture[]::new);

        this.future = CompletableFuture.allOf(parents) //Waiting for all parents to complete
                .thenCompose(v -> {
                    SearchSpace lhsSearchSpace = initializeSearchSpaceFor(this.edge.leftName);
                    SearchSpace rhsSearchSpace = initializeSearchSpaceFor(this.edge.rightName);

                    log.info(Utility.buildLog(String.format("S: %s", this), this.factory.getExecutor()));
                    return this.factory.run(this.edge, lhsSearchSpace, rhsSearchSpace);
                });

        //Once this node is completed crop it's parent lhs and rhs results.
        this.future.thenAccept(out -> {
            // Result handling belongs here
            log.info(Utility.buildLog(String.format("F: %s", this), this.factory.getExecutor()));

            results = out;
            //showResults(); //Testing Purposes
            if(children.isEmpty()){
                cropParentResults();
                log.debug("Finished Cropping All Parents: {}", this);
            }
        });
    }

    private SearchSpace initializeSearchSpaceFor(String variable){
        final boolean isLocked = this.parents.containsKey(variable);

        if (isLocked) {
            final ExecutionNode connectedNode = this.parents.get(variable);
            Result<?> nodeResults = connectedNode.getFuture().join(); //Safe join

            //Check the variable is matched with parent lhs variable.
            boolean isLockedWithLhs =  variable.equals(connectedNode.getEdge().leftName);

            //If isLockedWithLhs True then take lhs attributeSet list otherwise get rhs attributeSet list
            ObjectOpenHashSet<AttributeBitSet> lhsLockedSpace = isLockedWithLhs ? nodeResults.asLhsSet() : nodeResults.asRhsSet();
            return new SearchSpace.Locked(lhsLockedSpace);
        }else{
            return new SearchSpace.CC(this.relationMap.get(variable), this.level);
        }
    }

    private void cropParentResults(){
        for (Map.Entry<String, ExecutionNode> entry : this.parents.entrySet()) {
            if(entry.getKey().equals("#")){
                //this is anchor node parent dependency nothing to crop
                return;
            }

            ExecutionNode parent = entry.getValue();
            log.debug("Cropping Results Parent: {} by child: {}", parent, this);
            String parentLeftName = parent.getEdge().leftName;
            String parentRightName = parent.getEdge().rightName;

            String leftName = this.edge.leftName;
            String rightName = this.edge.rightName;

            if(parentLeftName.equals(leftName)) {
                parent.getResults().cropByLhsSet(this.getResults().asLhsSet());
            }

            if(parentRightName.equals(leftName)) {
                parent.getResults().cropByRhsSet(this.getResults().asLhsSet());
            }

            if(parentLeftName.equals(rightName)) {
                parent.getResults().cropByLhsSet(this.getResults().asRhsSet());
            }

            if(parentRightName.equals(rightName)) {
                parent.getResults().cropByRhsSet(this.getResults().asRhsSet());
            }

            parent.cropParentResults();
        }
    }

    public String getNodeId() {
        return this.edge + "_" + this.level;
    }

    public String getPreviousNodeId() {
        return this.edge + "_" + (this.level - 1);
    }

    @Override
    public String toString() {
        return getNodeId();
    }

    private String toJson(int indent) {
        String pad = "  ".repeat(indent);
        String childPad = "  ".repeat(indent + 1);

        StringBuilder sb = new StringBuilder();
        sb.append(pad).append("{\n");
        sb.append(childPad).append("\"node\": \"").append(getNodeId()).append("\"");

        if (!children.isEmpty()) {
            sb.append(",\n");
            sb.append(childPad).append("\"children\": [\n");
            for (int i = 0; i < children.size(); i++) {
                sb.append(children.get(i).toJson(indent + 2));
                if (i < children.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(childPad).append("]");
        }

        sb.append("\n").append(pad).append("}");
        return sb.toString();
    }
}

