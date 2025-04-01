package de.metaserve.model.result;

import de.metanome.Metanome;
import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.ColumnPermutation;
import de.metanome.algorithm_integration.results.FunctionalDependency;
import de.metanome.algorithm_integration.results.InclusionDependency;
import de.metanome.algorithm_integration.results.Result;
import de.metanome.algorithm_integration.results.UniqueColumnCombination;
import de.metanome.backend.result_postprocessing.results.InclusionDependencyResult;
import de.metaserve.model.dpal.FDEdge;
import de.metaserve.model.dpal.INDEdge;
import de.metaserve.model.dpal.UCCEdge;
import de.metaserve.model.graph.AggregateFunction;
import de.metaserve.util.common.Pair;
import de.metaserve.util.configuration.InputConfiguration;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciithemes.u8.U8_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import it.unimi.dsi.fastutil.Hash;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
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


    public void addFD(FDEdge edge) {
        String sourceColumn = edge.leftName;
        String targetColumn = edge.rightName;
        int sourceIndex = columnNames.indexOf(sourceColumn);
        int targetIndex = columnNames.indexOf(targetColumn);

        if (rows2.isEmpty()){
            for (Result result : edge.getResults()){
                FunctionalDependency dep = (FunctionalDependency) result;
                List<Set<ColumnIdentifier>> newRow = new ArrayList<>(columnNames.size());
                Pair<Set<ColumnIdentifier>, Set<ColumnIdentifier>> fd = fdFromDep(dep);
                for (int i = 0; i < columnNames.size(); i++){
                    if (sourceIndex == i)
                        newRow.add(fd.getFirst());
                    else if(targetIndex == i)
                        newRow.add(fd.getSecond());
                    else
                        newRow.add(null);
                }
                rows2.add(newRow);
            }
        } else {
            if ((!addedColumns.contains(sourceColumn)) && (!addedColumns.contains(targetColumn))){
                throw new RuntimeException("Result collection found an error!");
            } else if (addedColumns.contains(sourceColumn) && addedColumns.contains(targetColumn)){
                //throw new RuntimeException("Not implemented yet! (loop)");  @TODO
                System.out.println("Loop!");
                HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map = fdsToMap(edge.getResults());
                loopVerify(sourceIndex, targetIndex, map);
            } else if (addedColumns.contains(sourceColumn)) {
                HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map = fdsToMap(edge.getResults());
                addValuesInRow(targetIndex, sourceIndex, map);
            } else if (addedColumns.contains(targetColumn)) {
                HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map = fdsToMap(edge.getResults(), true);
                addValuesInRow(sourceIndex, targetIndex, map);
            }
        }
        addedColumns.add(sourceColumn);
        addedColumns.add(targetColumn);
    }

    private Pair<Set<ColumnIdentifier>, Set<ColumnIdentifier>> fdFromDep(FunctionalDependency dep) {
        return new Pair<>(ccsToSet(dep.getDeterminant()), idToSet(dep.getDependant()));
    }

    private Set<ColumnIdentifier> idToSet(ColumnIdentifier dependant) {
        Set<ColumnIdentifier> set = new HashSet<>();
        set.add(dependant);
        return set;
    }

    private Set<ColumnIdentifier> ccsToSet(ColumnCombination determinant) {
        return new HashSet<>(determinant.getColumnIdentifiers());
    }

    private HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> fdsToMap(List<Result> results) {
        return fdsToMap(results, false);
    }
    private HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> fdsToMap(List<Result> results, boolean reverse) {
        HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map = new HashMap<>();
        for (Result result : results){
            FunctionalDependency dep = (FunctionalDependency) result;
            Pair<Set<ColumnIdentifier>, Set<ColumnIdentifier>> ind = fdFromDep(dep);
            buildMapForDep(reverse, map, ind);
        }
        return map;
    }

    private void buildMapForDep(boolean reverse, HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map, Pair<Set<ColumnIdentifier>, Set<ColumnIdentifier>> ind) {
        Set<ColumnIdentifier> key = ind.getFirst();
        Set<ColumnIdentifier> value = ind.getSecond();
        if (reverse){
            key = ind.getSecond();
            value = ind.getFirst();
        }
        if (!map.containsKey(key)){
            map.put(key, new ArrayList<>());
        }
        if (!map.get(key).contains(value))
            map.get(key).add(value);
    }
    public void addUCC(UCCEdge edge) {
        String column = edge.leftName;
        int columnIndex = columnNames.indexOf(column);
        if (!rows2.isEmpty())
            throw new RuntimeException("Added UCC even though it is not requiered!");
        for (Result result : edge.getResults()){
            UniqueColumnCombination dep = (UniqueColumnCombination) result;
            List<Set<ColumnIdentifier>> newRow = new ArrayList<>(columnNames.size());
            Set<ColumnIdentifier> ccs = ccsToSet(dep.getColumnCombination());
            for (int i = 0; i < columnNames.size(); i++){
                if (columnIndex == i)
                    newRow.add(ccs);
                else
                    newRow.add(null);
            }
            rows2.add(newRow);
        }
        addedColumns.add(column);
    }

    public void addIND(INDEdge edge) {
        String sourceColumn = edge.leftName;
        String targetColumn = edge.rightName;
        int sourceIndex = columnNames.indexOf(sourceColumn);
        int targetIndex = columnNames.indexOf(targetColumn);

        if (rows2.isEmpty()){
            HashSet<Pair<Set<ColumnIdentifier>, Set<ColumnIdentifier>>> copies = new HashSet<>();
            for (Result result : edge.getResults()){
                InclusionDependency dep = (InclusionDependency) result;
                List<Set<ColumnIdentifier>> newRow = new ArrayList<>(columnNames.size());
                Pair<Set<ColumnIdentifier>, Set<ColumnIdentifier>> ind = indFromDep(dep);
                if (copies.contains(ind)) continue;
                copies.add(ind);
                for (int i = 0; i < columnNames.size(); i++){
                    if (sourceIndex == i)
                        newRow.add(ind.getFirst());
                    else if(targetIndex == i)
                        newRow.add(ind.getSecond());
                    else
                        newRow.add(null);
                }
                rows2.add(newRow);
            }
        } else {
            if ((!addedColumns.contains(sourceColumn)) && (!addedColumns.contains(targetColumn))){
                throw new RuntimeException("Result collection found an error!");
            } else if (addedColumns.contains(sourceColumn) && addedColumns.contains(targetColumn)){
                //throw new RuntimeException("Not implemented yet! (loop)"); @TODO
                System.out.println("Loop!");
                HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map = indsToMap(edge.getResults());
                loopVerify(sourceIndex, targetIndex, map);
            } else if (addedColumns.contains(sourceColumn)) {
                HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map = indsToMap(edge.getResults());
                addValuesInRow(targetIndex, sourceIndex, map);
            } else if (addedColumns.contains(targetColumn)) {
                HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map = indsToMap(edge.getResults(), true);
                addValuesInRow(sourceIndex, targetIndex, map);
            }
        }
        addedColumns.add(sourceColumn);
        addedColumns.add(targetColumn);
    }

    private void loopVerify(int sourceIndex, int targetIndex, HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map) {
        List<List<Set<ColumnIdentifier>>> newRows = new ArrayList<>();
        for (List<Set<ColumnIdentifier>> row : rows2){
            Set<ColumnIdentifier> columnCombinationTarget = row.get(targetIndex);
            Set<ColumnIdentifier> columnCombinationSource = row.get(sourceIndex);
            if (map.containsKey(columnCombinationSource)){
                if (map.get(columnCombinationSource).contains(columnCombinationTarget)){
                    newRows.add(row);
                }
            }
        }
        rows2 = newRows;
    }

    private HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> indsToMap(List<Result> results) {
        return indsToMap(results, false);
    }

    private HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> indsToMap(List<Result> results, boolean reverse) {
        HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map = new HashMap<>();
        for (Result result : results){
            InclusionDependency dep = (InclusionDependency) result;
            Pair<Set<ColumnIdentifier>, Set<ColumnIdentifier>> ind = indFromDep(dep);
            buildMapForDep(reverse, map, ind);
        }
        return map;
    }

    private Pair<Set<ColumnIdentifier>, Set<ColumnIdentifier>> indFromDep(InclusionDependency dep) {
        return new Pair<>(permutationToSet(dep.getDependant()), permutationToSet(dep.getReferenced()));
    }

    private Set<ColumnIdentifier> permutationToSet(ColumnPermutation dependant) {
        return new HashSet<>(dependant.getColumnIdentifiers());
    }

    private void addValuesInRow(int sourceIndex, int targetIndex, HashMap<Set<ColumnIdentifier>, List<Set<ColumnIdentifier>>> map) {
        List<List<Set<ColumnIdentifier>>> newRows = new ArrayList<>();
        for (List<Set<ColumnIdentifier>> row : rows2){
            Set<ColumnIdentifier> columnCombination = row.get(targetIndex);
            if (!map.containsKey(columnCombination)){
                continue;
                //throw new RuntimeException("Result Collection failed!"); @TODO
            }
            //row.add(targetIndex, columnCombination);
            for (Set<ColumnIdentifier> value : map.get(columnCombination)){
                List<Set<ColumnIdentifier>> newRow = new ArrayList<>(row);
                newRow.set(sourceIndex, value);
                newRows.add(newRow);
            }
        }
        rows2 = newRows;
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

    public void negativeUCC(String idName, String[] searchSpace) {
        List<Result> result = Metanome.getInstance().executeUCC(searchSpace);
        Set<Set<ColumnIdentifier>> uccs = uccsToSet(result);
        int id = columnNames.indexOf(idName);
        List<List<Set<ColumnIdentifier>>> newRows = new ArrayList<>();
        for (List<Set<ColumnIdentifier>> row : rows2){
            Set<ColumnIdentifier> columnCombination = row.get(id);
            if (!uccs.contains(columnCombination)){
                newRows.add(row);
            }
        }
        rows2 = newRows;
    }

    public Set<Set<ColumnIdentifier>> uccsToSet(List<Result> results){
        Set<Set<ColumnIdentifier>> set = new HashSet<>();
        for (Result result : results){
            UniqueColumnCombination dep = (UniqueColumnCombination) result;
            Set<ColumnIdentifier> cc = ccsToSet(dep.getColumnCombination());
            set.add(cc);
        }
        return set;
    }

    public HashMap<Set<ColumnIdentifier>, List<List<Set<ColumnIdentifier>>>> toMap(String columnKeyName){
        HashMap<Set<ColumnIdentifier>, List<List<Set<ColumnIdentifier>>>> map = new HashMap<>();
        int index = columnNames.indexOf(columnKeyName);
        for (List<Set<ColumnIdentifier>> row : rows2){
            Set<ColumnIdentifier> key = row.get(index);
            map.getOrDefault(key, new ArrayList<>()).add(row);
        }
        return map;
    }

    public void cardinality(String target, String source, String operation) {
        List<List<Set<ColumnIdentifier>>> newRows = new ArrayList<>();
        int targetID = columnNames.indexOf(target);
        int sourceID = columnNames.indexOf(source);
        for (List<Set<ColumnIdentifier>> row : rows2){
            Set<ColumnIdentifier> columnCombinationTarget = row.get(targetID);
            Set<ColumnIdentifier> columnCombinationSource = row.get(sourceID);
            long targetLong = InputConfiguration.getCard(columnCombinationTarget.stream().findFirst().get().toString());
            long sourceLong = InputConfiguration.getCard(columnCombinationSource.stream().findFirst().get().toString());

            if (targetLong > sourceLong * 0.3){
                newRows.add(row);
            }
        }
        rows2 = newRows;
    }
}
