package de.metaserve.model.constraints;

import de.metaserve.model.result.ResultSet;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
public class Contains extends AbstractMerger implements PostCondition {
    @Getter
    String x;
    @Getter
    String y;

    @Override
    public String getName() {
        return "CONTAINS";
    }

    @Override
    public String getLeft() {
        return x;
    }

    @Override
    public String getRight() {
        return y;
    }

    //ContainmentMerger
    @Override
    public void merge(ResultSet baseResultSet, ResultSet newResultSet) {
        mergeResultSets(baseResultSet, newResultSet, (key, value) -> {
            if (baseResultSet.getActiveColumns().contains(x))
                return key.containsAll(value);
            return value.containsAll(key);
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
                if (key.containsAll(value)){
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
}
