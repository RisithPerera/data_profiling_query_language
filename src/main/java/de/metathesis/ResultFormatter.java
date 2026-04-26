package de.metathesis;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.ColumnPermutation;
import de.metanome.algorithm_integration.results.InclusionDependency;
import de.metanome.algorithm_integration.results.MultivaluedDependency;
import de.metanome.algorithm_integration.results.UniqueColumnCombination;
import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.executor.min.graph.edge.FDEdge;
import de.metaserve.executor.min.graph.edge.INDEdge;
import de.metaserve.executor.min.graph.edge.UCCEdge;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.ExecutionNode;
import de.metathesis.structures.ResultTable;
import de.metathesis.structures.results.FDResult;
import de.metathesis.structures.results.INDResult;
import de.metathesis.structures.results.Result;
import de.metathesis.structures.results.UCCResult;

import java.util.*;

public class ResultFormatter {
    private static final ResultFormatter INSTANCE = new ResultFormatter();

    private final Preprocessor preprocessor;

    private ResultFormatter() {
        this.preprocessor = Preprocessor.getInstance();
    }

    public static ResultFormatter getInstance() {
        return INSTANCE;
    }

    public List<ResultTable> createResultSchema(Map<String, ExecutionNode> executionGraph) {
        List<ResultTable> schema = new ArrayList<>();
        Set<String> coveredVariables = new HashSet<>();

        Map<Edge, Result<?>> resultsByEdge = new LinkedHashMap<>();

        for (ExecutionNode node : executionGraph.values()) {
            Edge edge = node.getEdge();

            resultsByEdge.computeIfAbsent(edge, k -> {
                if (k instanceof FDEdge) return new FDResult();
                if (k instanceof INDEdge) return new INDResult();
                if (k instanceof UCCEdge) return new UCCResult();
                throw new IllegalStateException("Unknown edge type: " + k);
            });

            Result<?> result = resultsByEdge.get(edge);

            for (Object x : node.getResults()) {
                if (x instanceof FDResult.FD fd && result instanceof FDResult fdResult) {
                    fdResult.add(fd.lhs, fd.rhs); // dedup handled inside
                } else if (x instanceof INDResult.IND ind && result instanceof INDResult indResult) {
                    indResult.add(ind.lhs, ind.rhs);
                } else if (x instanceof UCCResult.UCC ucc && result instanceof UCCResult uccResult) {
                    uccResult.add(ucc.lhs);
                }
            }
        }

        // Create schema for binary dependencies like IND and FD
        for (Map.Entry<Edge, Result<?>> entry : resultsByEdge.entrySet()) {
            Edge edge = entry.getKey();
            if (edge instanceof UCCEdge) continue;

            String left = edge.leftName;
            String right = edge.rightName;

            boolean alreadyCovered = schema.stream().anyMatch(
                    t -> t.getColumnNames().contains(left) && t.getColumnNames().contains(right)
            );

            if (alreadyCovered) continue;

            ResultTable table = new ResultTable(List.of(left, right));

            Result<?> result = entry.getValue();
            if (result instanceof FDResult fdResult) {
                for (FDResult.FD fd : fdResult) {
                    table.addRow(List.of(format(fd.lhs), format(fd.rhs)));
                }
            } else if (result instanceof INDResult indResult) {
                for (INDResult.IND ind : indResult) {
                    table.addRow(List.of(format(ind.lhs), format(ind.rhs)));
                }
            }

            schema.add(table);
            coveredVariables.add(left);
            coveredVariables.add(right);
        }

        // Create schema for single dependencies
        for (Map.Entry<Edge, Result<?>> entry : resultsByEdge.entrySet()) {
            Edge edge = entry.getKey();
            if (!(edge instanceof UCCEdge)) continue;

            String var = edge.rightName;
            if (coveredVariables.contains(var)) continue;

            ResultTable table = new ResultTable(List.of(var));

            Result<?> result = entry.getValue();
            if (result instanceof UCCResult uccResult) {
                for (UCCResult.UCC ucc : uccResult) {
                    table.addRow(List.of(format(ucc.lhs)));
                }
            }

            schema.add(table);
            coveredVariables.add(var);
        }

        return schema;
    }

    public ResultTable join(ResultTable left, ResultTable right) {
        // Find shared column name
        String sharedColumn = null;
        int leftSharedIdx = -1;
        int rightSharedIdx = -1;

        for (int i = 0; i < left.getColumnNames().size(); i++) {
            for (int j = 0; j < right.getColumnNames().size(); j++) {
                if (left.getColumnNames().get(i).equals(right.getColumnNames().get(j))) {
                    sharedColumn = left.getColumnNames().get(i);
                    leftSharedIdx = i;
                    rightSharedIdx = j;
                    break;
                }
            }
            if (sharedColumn != null) break;
        }

        // Build result column names (merge, skip duplicate shared column)
        List<String> newColumns = new ArrayList<>(left.getColumnNames());
        for (int i = 0; i < right.getColumnNames().size(); i++) {
            if (i != rightSharedIdx) newColumns.add(right.getColumnNames().get(i));
        }

        ResultTable result = new ResultTable(newColumns);
        Set<List<List<String>>> seen = new LinkedHashSet<>(); // dedup

        if (sharedColumn == null) {
            for (List<List<String>> lRow : left.getRows()) {
                for (List<List<String>> rRow : right.getRows()) {
                    List<List<String>> merged = new ArrayList<>(lRow);
                    merged.addAll(rRow);
                    if (seen.add(merged)) result.addRow(merged);
                }
            }
        } else {
            for (List<List<String>> lRow : left.getRows()) {
                List<String> lKey = lRow.get(leftSharedIdx);
                for (List<List<String>> rRow : right.getRows()) {
                    List<String> rKey = rRow.get(rightSharedIdx);
                    if (lKey.equals(rKey)) {
                        List<List<String>> merged = new ArrayList<>(lRow);
                        for (int i = 0; i < rRow.size(); i++) {
                            if (i != rightSharedIdx) merged.add(rRow.get(i));
                        }
                        if (seen.add(merged)) result.addRow(merged);
                    }
                }
            }
        }

        return result;
    }

    private List<String> format(AttributeBitSet abs) {
        String relName = this.preprocessor.getRelation(abs.getRelationIndex()).getName();
        String[] cols = this.preprocessor.getRelation(abs.getRelationIndex()).getAttributeNames();

        List<String> result = new ArrayList<>();
        for (int i : abs.getAttributeIndexArray()) {
            result.add(relName + "." + cols[i]);
        }
        return result;
    }

    private String format2(AttributeBitSet abs) {

        String relName = this.preprocessor.getRelation(abs.getRelationIndex()).getName();
        String[] cols = this.preprocessor.getRelation(abs.getRelationIndex()).getAttributeNames();

        StringBuilder sb = new StringBuilder();
        sb.append(relName).append("(");

        BitSet bs = abs.getAttributeIndexSet();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            sb.append(cols[i]).append(';');
        }
        sb.setLength(sb.length() - 1);
        sb.append(")");
        return sb.toString();
    }

    public void printAttributeSet(AttributeBitSet... sets) {
        for (int i = 0; i < sets.length; i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(format(sets[i]));
        }
        System.out.println();
    }

    public ColumnIdentifier[] formatAbs(AttributeBitSet abs) {
        String relName = this.preprocessor.getRelation(abs.getRelationIndex()).getName();
        String[] cols = this.preprocessor.getRelation(abs.getRelationIndex()).getAttributeNames();

        ColumnIdentifier[] cdList = new ColumnIdentifier[abs.size()];

        int outIdx = 0;

        for (int i : abs.getAttributeIndexArray()) {
            cdList[outIdx++] = new ColumnIdentifier(relName, cols[i]);
        }

        return cdList;
    }

    public UniqueColumnCombination formatUCC(UCCResult.UCC ucc) {
        return new UniqueColumnCombination(formatAbs(ucc.lhs));
    }

    public InclusionDependency formatIND(INDResult.IND ind) {
        ColumnPermutation dependant = new ColumnPermutation(formatAbs(ind.lhs));
        ColumnPermutation referenced = new ColumnPermutation(formatAbs(ind.rhs));
        return new InclusionDependency(dependant, referenced);
    }

    public MultivaluedDependency formatFD(FDResult.FD fd) {
        ColumnCombination determinant = new ColumnCombination(formatAbs(fd.lhs));
        ColumnCombination dependant = new ColumnCombination(formatAbs(fd.rhs));
        return new MultivaluedDependency(determinant, dependant);
    }

    public void printUCC(UCCResult result) {
        result.forEach(ucc -> System.out.println(format(ucc.lhs)));
    }

    public void printIND(INDResult result) {
        result.forEach(ind -> System.out.println(format(ind.lhs) + " ⊆ " + format(ind.rhs)));
    }

    public void printFD(FDResult result) {
        result.forEach(fd -> System.out.println(format(fd.lhs) + " → " + format(fd.rhs)));
    }
}
