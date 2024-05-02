package de.metaserve.model.result;

import de.metaserve.model.graph.AggregateFunction;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciithemes.u8.U8_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

import java.util.*;
import java.util.function.Function;

public class ResultSet implements Collection<List<String>> {
    private List<String> columnNames;
    private List<List<String>> rows;

    public ResultSet(List<String> columnNames) {
        this.columnNames = columnNames;
        this.rows = new ArrayList<>();
    }

    public void orderBy(List<String> columnNames) {
        List<Integer> columnIndices = getColumnIndices(columnNames);

        if (columnIndices.size() != columnNames.size()) {
            //@TODO throw error
            return;
        }

        Comparator<List<String>> comparator = (row1, row2) -> {
            for (int index : columnIndices) {
                int result = row1.get(index).compareTo(row2.get(index));
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };

        rows.sort(comparator);

    }

    private List<Integer> getColumnIndices(List<String> columnNames) {
        List<Integer> columnIndices = new ArrayList<>();

        for (String columnName : columnNames) {
            int index = this.columnNames.indexOf(columnName);
            if (index != -1) {
                columnIndices.add(index);
            }
        }

        return columnIndices;
    }
    public void groupBy(List<String> groupByColumns){
        groupBy(groupByColumns, new HashMap<>());
    }
    public void groupBy(List<String> groupByColumns, Map<String, AggregateFunction> aggregationFunctions) {
        List<Integer> groupByIndices = getColumnIndices(groupByColumns);

        if (groupByIndices.size() != groupByColumns.size()) {
            System.out.println("One or more group by columns do not exist.");
            return;
        }

        Map<List<String>, List<List<String>>> groupedData = new HashMap<>();

        for (List<String> row : rows) {
            List<String> groupValues = extractGroupValues(row, groupByIndices);

            if (!groupedData.containsKey(groupValues)) {
                groupedData.put(groupValues, new ArrayList<>());
            }

            groupedData.get(groupValues).add(row);
        }
        this.rows.clear();
        for (List<String> groupValues : groupedData.keySet()) {
            //System.out.println("Group: " + groupValues);
            //System.out.println(groupedData.get(groupValues));
            List<List<String>> groupRows = groupedData.get(groupValues);
            List<String> newRow = new ArrayList<>();

            for (String columnName : columnNames) {
                AggregateFunction aggregationFunction = aggregationFunctions.getOrDefault(columnName, AggregateFunction.FIRST);
                List<String> columnValues = getColumnValues(groupRows, columnName);
                String aggregatedValue = aggregationFunction.apply(columnValues);
                //System.out.println("Aggregated " + columnName + ": " + aggregatedValue);
                newRow.add(aggregatedValue);
            }
            this.rows.add(newRow);
            //System.out.println("--------------------");
        }
    }

    private List<String> getColumnValues(List<List<String>> rows, String columnName) {
        int columnIndex = columnNames.indexOf(columnName);
        List<String> columnValues = new ArrayList<>();

        if (columnIndex != -1) {
            for (List<String> row : rows) {
                if (columnIndex < row.size()) {
                    columnValues.add(row.get(columnIndex));
                }
            }
        }

        return columnValues;
    }

    private List<List<String>> convertToRows(Map<List<String>, List<List<String>>> groupedData) {
        List<List<String>> newRows = new ArrayList<>();

        for (List<List<String>> groupRows : groupedData.values()) {
            newRows.addAll(groupRows);
        }

        return newRows;
    }
    private List<String> extractGroupValues(List<String> row, List<Integer> groupByIndices) {
        List<String> groupValues = new ArrayList<>();

        for (int index : groupByIndices) {
            if (index < row.size()) {
                groupValues.add(row.get(index));
            }
        }

        return groupValues;
    }

    public void select(List<String> selectedNames) {
        List<Integer> selectedIndices = new ArrayList<>();

        // Find the indices of the selected column names
        for (String name : selectedNames) {
            int index = columnNames.indexOf(name);
            if (index != -1) {
                selectedIndices.add(index);
            }
        }

        // Remove non-selected columns from columnNames list
        columnNames.retainAll(selectedNames);

        // Remove non-selected values from each row
        for (List<String> row : rows) {
            for (int i = row.size() - 1; i >= 0; i--) {
                if (!selectedIndices.contains(i)) {
                    row.remove(i);
                }
            }
        }
    }

    public void dropColumn(String columnName) {
        int columnIndex = columnNames.indexOf(columnName);

        if (columnIndex == -1) {
            return;
        }

        for (List<String> row : rows) {
            if (columnIndex < row.size()) {
                row.remove(columnIndex);
            }
        }

        columnNames.remove(columnIndex);

    }

    public ResultSet join(ResultSet other) {
        // Find the common column name
        String joinColumnName = null;

        for (String columnName : columnNames) {
            if (other.columnNames.contains(columnName)) {
                joinColumnName = columnName;
                break;
            }
        }

        if (joinColumnName == null) {
            return null;
        }

        // Get the index of the join column in both ResultSets
        int joinColumnIndex = columnNames.indexOf(joinColumnName);
        int otherJoinColumnIndex = other.columnNames.indexOf(joinColumnName);

        // Create the combined column names
        List<String> combinedColumnNames = new ArrayList<>(columnNames);
        combinedColumnNames.addAll(other.columnNames);
        combinedColumnNames.remove(joinColumnName);

        ResultSet result = new ResultSet(combinedColumnNames);

        // Perform the join operation
        for (List<String> row : rows) {
            String joinColumnValue = row.get(joinColumnIndex);

            for (List<String> otherRow : other.rows) {
                String otherJoinColumnValue = otherRow.get(otherJoinColumnIndex);

                if (joinColumnValue.equals(otherJoinColumnValue)) {
                    List<String> combinedRow = new ArrayList<>(row);
                    combinedRow.addAll(otherRow);
                    combinedRow.remove(otherJoinColumnValue);
                    result.add(combinedRow);
                }
            }
        }

        return result;
    }

    public ResultSet multiJoin(ResultSet other) {
        // Find the common column names
        List<String> commonColumnNames = new ArrayList<>();

        for (String columnName : columnNames) {
            if (other.columnNames.contains(columnName)) {
                commonColumnNames.add(columnName);
            }
        }

        if (commonColumnNames.isEmpty()) {
            return null;
        }

        // Get the indices of the join columns in both ResultSets
        List<Integer> joinColumnIndices = new ArrayList<>();
        List<Integer> otherJoinColumnIndices = new ArrayList<>();

        for (String joinColumnName : commonColumnNames) {
            int joinColumnIndex = columnNames.indexOf(joinColumnName);
            int otherJoinColumnIndex = other.columnNames.indexOf(joinColumnName);

            joinColumnIndices.add(joinColumnIndex);
            otherJoinColumnIndices.add(otherJoinColumnIndex);
        }

        // Create the combined column names
        List<String> combinedColumnNames = new ArrayList<>(columnNames);
        combinedColumnNames.addAll(other.columnNames);

        for (String joinColumnName : commonColumnNames) {
            combinedColumnNames.remove(joinColumnName);
        }

        ResultSet result = new ResultSet(combinedColumnNames);

        // Perform the join operation
        for (List<String> row : rows) {
            List<String> joinColumnValues = new ArrayList<>();

            for (int joinColumnIndex : joinColumnIndices) {
                joinColumnValues.add(row.get(joinColumnIndex));
            }

            for (List<String> otherRow : other.rows) {
                List<String> otherJoinColumnValues = new ArrayList<>();

                for (int otherJoinColumnIndex : otherJoinColumnIndices) {
                    otherJoinColumnValues.add(otherRow.get(otherJoinColumnIndex));
                }

                if (joinColumnValues.equals(otherJoinColumnValues)) {
                    List<String> combinedRow = new ArrayList<>(row);
                    combinedRow.addAll(otherRow);

                    for (int index : joinColumnIndices) {
                        combinedRow.remove(index);
                    }

                    result.add(combinedRow);
                }
            }
        }

        return result;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    @Override
    public int size() {
        return rows.size();
    }

    @Override
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return rows.contains(o);
    }

    @Override
    public Iterator<List<String>> iterator() {
        return rows.iterator();
    }

    @Override
    public Object[] toArray() {
        return rows.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return rows.toArray(a);
    }

    @Override
    public boolean add(List<String> row) {
        if (row.size() != columnNames.size()) {
            throw new IllegalArgumentException("Row size does not match column count");
        }
        return rows.add(row);
    }

    @Override
    public boolean remove(Object o) {
        return rows.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return rows.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends List<String>> c) {
        return rows.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return rows.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return rows.retainAll(c);
    }

    @Override
    public void clear() {
        rows.clear();
    }

    public boolean canJoin(ResultSet joinCanidate) {
        String joinColumnName = null;
        for (String columnName : columnNames) {
            if (joinCanidate.columnNames.contains(columnName)) {
                joinColumnName = columnName;
                break;
            }
        }

        return joinColumnName != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultSet resultSet = (ResultSet) o;
        return Objects.equals(columnNames, resultSet.columnNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnNames);
    }

    @Override
    public String toString() {
        AsciiTable asciiTable = new AsciiTable();
        asciiTable.addRule();

        // Add column names
        asciiTable.addRow(this.columnNames);
        asciiTable.addRule();
        asciiTable.addRule();

        // Add rows
        for (List<String> row : rows) {
            asciiTable.addRow(row);
            asciiTable.addRule();
        }

        // Set table theme
        asciiTable.getContext().setGrid(U8_Grids.borderDouble());
        asciiTable.setTextAlignment(TextAlignment.CENTER);

        return asciiTable.render();
    }
}
