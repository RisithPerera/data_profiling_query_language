package de.metaserve.util.extensions.fk;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metaserve.util.result.ResultSet;
import de.metaserve.util.singletons.InputConfigurationSingleton;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForeignKeyChecker {
    Set<ForeignKey> fkListGoldStandard;
    Set<ColumnCombination> fksGoldStandard = new HashSet<>();
    Set<ColumnCombination> pksGoldStandard = new HashSet<>();

    public ForeignKeyChecker(){
        load();
    }

    public void check(ResultSet resultSet){
        Set<ForeignKey> fkList = convert(resultSet);
        //Set<ColumnCombination> fkCCList = convertCC(fkList);
        //compareForeignKeys(fksGoldStandard, fkCCList);
        Set<ColumnCombination> pkCCList = convertCCtoPk(fkList);
        compareForeignKeys(pksGoldStandard, pkCCList);
        compareForeignRelationShip(fkListGoldStandard, fkList);
    }

    private Set<ColumnCombination> convertCCtoPk(Set<ForeignKey> resultSet) {
        Set<ColumnCombination> fkList = new HashSet<>();
        for (ForeignKey fk : resultSet){
            ColumnCombination cc = new ColumnCombination();
            for (String col : fk.pk_column){
                cc.getColumnIdentifiers().add(new ColumnIdentifier(fk.pk_table, col));
            }
            fkList.add(cc);
        }
        return fkList;
    }

    public void compareForeignKeys(Set<ColumnCombination> trueList, Set<ColumnCombination> actualList) {
        int equalMatches = 0;
        int falsePositives = 0;
        int falseNegatives = 0;


        for (ColumnCombination trueKey : trueList) {
            if(actualList.contains(trueKey)){
                equalMatches++;
            } else {
                falseNegatives++;
            }
        }

        for (ColumnCombination actualKey : actualList) {
            if(!trueList.contains(actualKey)) {
                falsePositives++;
                //System.out.println(actualKey);
            }
        }


        System.out.println("Keys:" + actualList.size());
        System.out.println("Equal Matches: " + equalMatches);
        System.out.println("False Positives/Too much: " + falsePositives);
        System.out.println("False Negatives/Missing: " + falseNegatives);
        System.out.println();
        double recall = computeRecall(equalMatches, falseNegatives);
        double precision = computePrecision(equalMatches, falsePositives);
        System.out.println("F1: " + computeF1Score(precision, recall));
        System.out.println("Recall: " + recall);
        System.out.println("Precision: " + precision);
        System.out.println();
    }

    public void compareForeignRelationShip(Set<ForeignKey> trueList, Set<ForeignKey> actualList) {
        int equalMatches = 0;
        int falsePositives = 0;
        int falseNegatives = 0;

/*
        for (ForeignKey trueKey : trueList) {
            //System.out.println("["+trueKey.pk_table.toLowerCase()+".csv."+trueKey.pk_column.get(0).toLowerCase()+"] --> ["+trueKey.fk_table.toLowerCase()+".csv."+trueKey.fk_column.get(0).toLowerCase()+"]");
            System.out.println("["+trueKey.fk_table.toLowerCase()+".csv."+trueKey.fk_column.get(0).toLowerCase()+"] --> ["+trueKey.pk_table.toLowerCase()+".csv."+trueKey.pk_column.get(0).toLowerCase()+"]");
        }
 */

        //System.out.println(trueList);
        //System.out.println(actualList);
        for (ForeignKey trueKey : trueList) {

            if(actualList.contains(trueKey)){
                equalMatches++;
                //System.out.println(trueKey);
            } else {
                falseNegatives++;
                System.out.println(trueKey);
                //System.out.println("["+trueKey.fk_table.toLowerCase()+".csv."+trueKey.fk_column.get(0).toLowerCase()+"] --> ["+trueKey.pk_table.toLowerCase()+".csv."+trueKey.pk_column.get(0).toLowerCase()+"]");

            }

        }

        for (ForeignKey actualKey : actualList) {

            if(!trueList.contains(actualKey)) {
                falsePositives++;
                //System.out.println(actualKey);
            }


        }

        System.out.println("Relationships:" + actualList.size());
        System.out.println("Equal Matches: " + equalMatches);
        System.out.println("False Positives/Too much: " + falsePositives);
        System.out.println("False Negatives/Missing: " + falseNegatives);
        System.out.println();
        double recall = computeRecall(equalMatches, falseNegatives);
        double precision = computePrecision(equalMatches, falsePositives);
        System.out.println("F1: " + computeF1Score(precision, recall));
        System.out.println("Recall: " + recall);
        System.out.println("Precision: " + precision);
        System.out.println();
    }

    public double computePrecision(int trueMatches, int wrongMatches) {
        return (double) trueMatches / (trueMatches + wrongMatches);
    }

    public double computeRecall(int trueMatches, int missedMatches) {
        return (double) trueMatches / (trueMatches + missedMatches);
    }

    public double computeF1Score(double precision, double recall) {
        return 2 * ((precision * recall) / (precision + recall));
    }

    private Set<ForeignKey> convert(ResultSet resultSet) {
        Set<ForeignKey> fkList = new HashSet<>();
        for (List<Set<ColumnIdentifier>> row : resultSet.getRows2()){
            fkList.add(resultRowSetToForeignKey(row));
        }
        return fkList;
    }

    private Set<ColumnCombination> convertCC(Set<ForeignKey> resultSet) {
        Set<ColumnCombination> fkList = new HashSet<>();
        for (ForeignKey fk : resultSet){
            ColumnCombination cc = new ColumnCombination();
            for (String col : fk.fk_column){
                cc.getColumnIdentifiers().add(new ColumnIdentifier(fk.fk_table, col));
            }
            fkList.add(cc);
        }
        return fkList;
    }

    public ForeignKey resultRowSetToForeignKey(List<Set<ColumnIdentifier>> row){
        ForeignKey foreignKey = new ForeignKey();
        if(row.size() != 2)
            throw new RuntimeException("Could not build FK from " + row);
        List<ColumnIdentifier> fk = row.get(0).stream().toList();
        List<ColumnIdentifier> pk = row.get(1).stream().toList();

        foreignKey.parse(fk,pk);
        return foreignKey;
    }

    public ForeignKey resultRowToForeignKey(List<String> row){
        ForeignKey foreignKey = new ForeignKey();
        if(row.size() != 2)
            throw new RuntimeException("Could not build FK from " + row);
        String fk = row.get(0);
        String pk = row.get(1);

        foreignKey.parse(fk,pk);
        return foreignKey;
    }

    public void load(){
        String path = InputConfigurationSingleton.get().getMetaDataPath() + "ForeignKeys.csv";
        try {
            fkListGoldStandard = parseCSV(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (ForeignKey fk : fkListGoldStandard){
            ColumnCombination cc = new ColumnCombination();
            for (String col : fk.fk_column){
                cc.getColumnIdentifiers().add(new ColumnIdentifier(fk.fk_table, col));
            }
            fksGoldStandard.add(cc);

            ColumnCombination ccPK = new ColumnCombination();
            for (String col : fk.pk_column){
                ccPK.getColumnIdentifiers().add(new ColumnIdentifier(fk.pk_table, col));
            }
            pksGoldStandard.add(ccPK);
        }
    }

    public Set<ForeignKey> parseCSV(String filePath) throws IOException {
        Set<ForeignKey> foreignKeys = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            //Skip header
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(";");

                ForeignKey foreignKey = new ForeignKey();
                foreignKey.setFkSchema(values[0]);
                foreignKey.setFkTable(values[1]);
                foreignKey.setFkColumn(values[2].split(","));
                foreignKey.setPkSchema(values[3]);
                foreignKey.setPkTable(values[4]);
                foreignKey.setPkColumn(values[5].split(","));

                foreignKeys.add(foreignKey);
            }
        }

        return foreignKeys;
    }
}
