package de.metaserve.util.extensions.mapping;

import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metaserve.model.result.ResultSet;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONArray;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class MapperChecker {

    Map<ColumnIdentifier, ColumnIdentifier> mapping = new HashMap<>();
    public MapperChecker(){
        this.load();
    }

    public void load(){
        String path = InputConfigurationSingleton.get().getInputPath() + "mapping.json";
        parseJson(path);
    }

    public void parseJson(String filePath){
        JSONParser parser = new JSONParser();

        try (FileReader reader = new FileReader(filePath)) {
            // Parse the JSON file into a JSON array
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            JSONArray jsonArray = (JSONArray) jsonObject.get("matches");
            // Iterate over the JSON array
            for (Object obj : jsonArray) {
                JSONObject matchObj = (JSONObject) obj;

                // Extract the values from each match object
                String sourceTable = (String) matchObj.get("source_table");
                String sourceColumn = (String) matchObj.get("source_column");
                String targetTable = (String) matchObj.get("target_table");
                String targetColumn = (String) matchObj.get("target_column");

                // Create ColumnCombination objects
                ColumnIdentifier source = new ColumnIdentifier(sourceTable, sourceColumn);
                ColumnIdentifier target = new ColumnIdentifier(targetTable, targetColumn);

                // Add the match to the map
                mapping.put(source, target);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    public void checkFD(ResultSet resultSet) {
        Map<ColumnIdentifier, List<ColumnIdentifier>> actualMap = convertFD(resultSet);
        check(actualMap);
    }

    public void check(ResultSet resultSet) {
        Map<ColumnIdentifier, List<ColumnIdentifier>> actualMap = convert(resultSet);
        check(actualMap);
    }
    public void check(Map<ColumnIdentifier, List<ColumnIdentifier>> actualMap){
        int equalMatches = 0;
        int falsePositives = 0;
        int falseNegatives = 0;

/*
        for (ForeignKey trueKey : trueList) {
            //System.out.println("["+trueKey.pk_table.toLowerCase()+".csv."+trueKey.pk_column.get(0).toLowerCase()+"] --> ["+trueKey.fk_table.toLowerCase()+".csv."+trueKey.fk_column.get(0).toLowerCase()+"]");
            System.out.println("["+trueKey.fk_table.toLowerCase()+".csv."+trueKey.fk_column.get(0).toLowerCase()+"] --> ["+trueKey.pk_table.toLowerCase()+".csv."+trueKey.pk_column.get(0).toLowerCase()+"]");
        }
 */

        for (ColumnIdentifier trueKey : mapping.keySet()) {
            ColumnIdentifier leftSide = mapping.get(trueKey);
            if(actualMap.containsKey(trueKey) && actualMap.get(trueKey).contains(leftSide)){
                equalMatches++;
            } else {
                falseNegatives++;
                //System.out.println(trueKey);
                //System.out.println("["+trueKey.fk_table.toLowerCase()+".csv."+trueKey.fk_column.get(0).toLowerCase()+"] --> ["+trueKey.pk_table.toLowerCase()+".csv."+trueKey.pk_column.get(0).toLowerCase()+"]");

            }

        }

        for (ColumnIdentifier actualKey : actualMap.keySet()) {
            List<ColumnIdentifier> ciList = actualMap.get(actualKey);
            if(mapping.containsKey(actualKey)){
                for (ColumnIdentifier ci : ciList) {
                    if(!mapping.get(actualKey).equals(ci)) {
                        falsePositives++;
                        //System.out.println(actualKey);
                    }
                }
            } else {
                falsePositives += ciList.size();
            }
        }

        System.out.println("Relationships:" + actualMap.size());
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


    private Map<ColumnIdentifier, List<ColumnIdentifier>> convertFD(ResultSet resultSet) {
        Map<ColumnIdentifier, List<ColumnIdentifier>> map = new HashMap<>();
        for (List<String> row : resultSet.getRows()){
            ColumnIdentifier left = convertStringToCI(row.get(1));
            ColumnIdentifier right = convertStringToCI(row.get(0));
            if(!map.containsKey(left))
                map.put(left, new ArrayList<>());
            map.get(left).add(right);
        }
        return map;
    }

    private Map<ColumnIdentifier, List<ColumnIdentifier>> convert(ResultSet resultSet) {
        Map<ColumnIdentifier, List<ColumnIdentifier>> map = new HashMap<>();
        for (List<String> row : resultSet.getRows()){
            ColumnIdentifier left = convertStringToCI(row.get(1));
            ColumnIdentifier right = convertStringToCI(row.get(0));
            if(!map.containsKey(left))
                map.put(left, new ArrayList<>());
            map.get(left).add(right);
        }
        return map;
    }

    private ColumnIdentifier convertStringToCI(String s) {
        s = s.replace("[", "").replace("]","");
        String[] split = s.split("\\.csv\\.");
        return new ColumnIdentifier(split[0], split[1]);
    }
}
