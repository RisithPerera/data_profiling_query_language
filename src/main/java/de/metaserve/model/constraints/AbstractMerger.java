package de.metaserve.model.constraints;

import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metaserve.model.result.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

public abstract class AbstractMerger implements Mergeable {

    protected void mergeResultSets(
            ResultSet baseResultSet,
            ResultSet newResultSet,
            BiPredicate<Set<ColumnIdentifier>, Set<ColumnIdentifier>> condition) {

        String baseName = getLeft();
        String newName = getRight();

        if (!baseResultSet.getActiveColumns().contains(baseName)) {
            baseName = getRight();
            newName = getLeft();
        }

        int baseIndex = baseResultSet.getColumnNames().indexOf(baseName);
        int newIndex = newResultSet.getColumnNames().indexOf(newName);

        List<List<Set<ColumnIdentifier>>> mergedRows = new ArrayList<>();

        for (List<Set<ColumnIdentifier>> row1 : baseResultSet.getRows2()) {
            Set<ColumnIdentifier> key = row1.get(baseIndex);
            for (List<Set<ColumnIdentifier>> row2 : newResultSet.getRows2()) {
                Set<ColumnIdentifier> value = row2.get(newIndex);
                if (condition.test(key, value)) {
                    List<Set<ColumnIdentifier>> mergedRow = mergeRows(row1, row2);
                    mergedRows.add(mergedRow);
                }
            }
        }

        baseResultSet.setRows2(mergedRows);
        baseResultSet.getActiveColumns().addAll(newResultSet.getActiveColumns());
    }

    private List<Set<ColumnIdentifier>> mergeRows(
            List<Set<ColumnIdentifier>> row1,
            List<Set<ColumnIdentifier>> row2) {

        List<Set<ColumnIdentifier>> newRow = new ArrayList<>();
        int size = Math.max(row1.size(), row2.size());

        for (int i = 0; i < size; i++) {
            Set<ColumnIdentifier> cell1 = i < row1.size() ? row1.get(i) : null;
            Set<ColumnIdentifier> cell2 = i < row2.size() ? row2.get(i) : null;

            if (cell1 != null) {
                newRow.add(cell1);
            } else if (cell2 != null) {
                newRow.add(cell2);
            } else {
                newRow.add(null);
            }
        }

        return newRow;
    }

}
