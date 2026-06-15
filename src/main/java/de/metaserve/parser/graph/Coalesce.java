package de.metaserve.parser.graph;

import de.metaserve.input.ColumnIdentifier;
import de.metaserve.util.result.ResultSet;

import java.util.Optional;

public class Coalesce extends AbstractMerger implements PostCondition {

    String x;

    String y;

    public Coalesce(String x, String y){
        this.x = x;
        this.y = y;
    }
    @Override
    public String getName() {
        return "COALESCE";
    }

    @Override
    public String getLeft() {
        return x;
    }

    @Override
    public String getRight() {
        return y;
    }

    //TableIdentifierMerger
    @Override
    public void merge(ResultSet baseResultSet, ResultSet newResultSet) {
        mergeResultSets(baseResultSet, newResultSet, (key, value) -> {
            Optional<ColumnIdentifier> keyColumn = key.stream().findAny();
            Optional<ColumnIdentifier> valueColumn = value.stream().findAny();
            return keyColumn.isPresent() && valueColumn.isPresent()
                    && keyColumn.get().getTableIdentifier().equals(valueColumn.get().getTableIdentifier());
        });
    }
    /*@Override
    public void merge(ResultSet baseResultSet, ResultSet newResultSet) {
        String baseName = x;
        String newName = y;
        if (!baseResultSet.getActiveColumns().contains(x)){
            baseName = y;
            newName = x;
        }
        int baseIndex = baseResultSet.getColumnNames().indexOf(baseName);
        int newIndex = newResultSet.getColumnNames().indexOf(newName);

        List<List<Set<ColumnIdentifier>>> newRows = new ArrayList<>();
        for (List<Set<ColumnIdentifier>> row1 : baseResultSet.getRows2()){
            Set<ColumnIdentifier> key = row1.get(baseIndex);
            for (List<Set<ColumnIdentifier>> row2 : newResultSet.getRows2()){
                Set<ColumnIdentifier> value = row2.get(newIndex);
                if (key.stream().findAny().get().getTableIdentifier().equals(value.stream().findAny().get().getTableIdentifier())){
                    List<Set<ColumnIdentifier>> newRow = new ArrayList<>();
                    for (int i = 0; i < row1.size(); i++) {
                        if (row1.get(i) != null){
                            newRow.add(row1.get(i));
                        } else if(row2.get(i) != null){
                            newRow.add(row2.get(i));
                        } else {
                            newRow.add(null);
                        }
                    }
                    newRows.add(newRow);
                }
            }
        }
        baseResultSet.setRows2(newRows);
        baseResultSet.getActiveColumns().addAll(newResultSet.getActiveColumns());
    }

     */

    @Override
    public String toString() {
        return getName() + "("+ getLeft() + "," + getRight() + ")";
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }
}
