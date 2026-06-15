package de.metaserve.util.result;

import de.metaserve.input.ColumnIdentifier;
import de.metaserve.util.configuration.InputConfiguration;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciithemes.u8.U8_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

import java.util.*;
import java.util.stream.Collectors;

public class ResultSet implements Collection<List<String>> {

    private List<String> columnNames;
    private Set<String> addedColumns = new HashSet<>();
    private List<List<String>> rows;
    private List<List<Set<ColumnIdentifier>>> rows2;

    public ResultSet(List<String> columnNames) {
        this.columnNames = columnNames;
        this.rows = new ArrayList<>();
        this.rows2 = new ArrayList<>();
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

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public List<List<Set<ColumnIdentifier>>> getRows2() {
        return rows2;
    }

    public void setRows2(List<List<Set<ColumnIdentifier>>> newRows){
        this.rows2 = newRows;
    }

    @Override
    public int size() {
        return rows2.size();
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


    public Set<String> getActiveColumns(){
        return addedColumns;
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
        for (List<Set<ColumnIdentifier>> row : getRows2()) {
            List<String> displayRow = new ArrayList<>(columnNames.size());
            for (int i = 0; i < columnNames.size(); i++) {
                Set<ColumnIdentifier> cell =
                        (row != null && i < row.size()) ? row.get(i) : null;
                displayRow.add(formatCell(cell));
            }
            asciiTable.addRow(displayRow);
            asciiTable.addRule();
        }

        // Set table theme
        asciiTable.getContext().setGrid(U8_Grids.borderDouble());
        asciiTable.setTextAlignment(TextAlignment.CENTER);

        return asciiTable.render();
    }

    private String formatCell(Set<ColumnIdentifier> cell) {
        if (cell == null || cell.isEmpty()) return "";
        return cell.stream()
                .map(ci -> {
                    // Prefer "table.column" if available; fall back to ci.toString()
                    String table = Objects.toString(ci.getTableIdentifier(), "");
                    String col   = Objects.toString(ci.getColumnIdentifier(), "");
                    if (!table.isEmpty() && !col.isEmpty()) return table + "." + col;
                    return ci.toString();
                })
                .sorted()
                .collect(Collectors.joining(" ∧ "));
    }

    public void contains(String x, String y) {
        List<List<Set<ColumnIdentifier>>> newRows = new ArrayList<>();
        int targetID = columnNames.indexOf(x);
        int sourceID = columnNames.indexOf(y);
        for (List<Set<ColumnIdentifier>> row : rows2){
            Set<ColumnIdentifier> columnCombinationTarget = row.get(targetID);
            Set<ColumnIdentifier> columnCombinationSource = row.get(sourceID);
            if (columnCombinationTarget.containsAll(columnCombinationSource)){
                newRows.add(row);
            }
        }
        rows2 = newRows;
    }

    public void split(String x, String y) {
        List<List<Set<ColumnIdentifier>>> newRows = new ArrayList<>();
        int targetID = columnNames.indexOf(x);
        int sourceID = columnNames.indexOf(y);
        for (List<Set<ColumnIdentifier>> row : rows2){
            Set<ColumnIdentifier> columnCombinationTarget = row.get(targetID);
            Set<ColumnIdentifier> columnCombinationSource = row.get(sourceID);
            if (!columnCombinationTarget.iterator().next().getTableIdentifier().equals(columnCombinationSource.iterator().next().getTableIdentifier())){
                newRows.add(row);
            }
        }
        rows2 = newRows;
    }

    public void coalesce(String x, String y) {
        List<List<Set<ColumnIdentifier>>> newRows = new ArrayList<>();
        int targetID = columnNames.indexOf(x);
        int sourceID = columnNames.indexOf(y);
        for (List<Set<ColumnIdentifier>> row : rows2){
            Set<ColumnIdentifier> columnCombinationTarget = row.get(targetID);
            Set<ColumnIdentifier> columnCombinationSource = row.get(sourceID);
            if (columnCombinationTarget.iterator().next().getTableIdentifier().equals(columnCombinationSource.iterator().next().getTableIdentifier())){
                newRows.add(row);
            }
        }
        rows2 = newRows;
    }
}
