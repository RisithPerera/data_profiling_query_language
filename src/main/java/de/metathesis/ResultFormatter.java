package de.metathesis;

import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.executor.min.graph.edge.UCCEdge;
import de.metathesis.profilers.results.FDResult;
import de.metathesis.profilers.results.INDResult;
import de.metathesis.profilers.results.Result;
import de.metathesis.profilers.results.UCCResult;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.ResultTable;

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

    public List<ResultTable> createResultSchema(Map<Edge, Result<?>> resultsByEdge) {
        List<ResultTable> schema = new ArrayList<>();
        Set<String> coveredVariables = new HashSet<>();

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
        Set<List<List<String>>> seen = new LinkedHashSet<>();

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
}
