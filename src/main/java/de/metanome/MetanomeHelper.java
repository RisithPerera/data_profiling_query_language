package de.metanome;

import de.metanome.algorithm_integration.AlgorithmConfigurationException;
import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.ColumnPermutation;
import de.metanome.algorithm_integration.input.InputGenerationException;
import de.metanome.algorithm_integration.input.RelationalInput;
import de.metanome.algorithm_integration.input.RelationalInputGenerator;
import de.metanome.algorithm_integration.results.*;
import de.metanome.algorithm_integration.results.basic_statistic_values.BasicStatisticValueLong;
import de.metanome.algorithms.dva.DVA;
import de.metanome.algorithms.hyfd.HyFD;
import de.metanome.algorithms.hyucc.HyUCC;
import de.metanome.backend.result_receiver.ResultCache;
import de.metanome.backend.result_receiver.ResultReceiver;
import de.metanome.DependencyType;
import de.metaserve.util.singletons.InputConfigurationSingleton;
import de.uni_potsdam.hpi.utils.CollectionUtils;
import de.uni_potsdam.hpi.utils.FileUtils;

import java.io.IOException;
import java.util.*;

public class MetanomeHelper {

    public static DVA createDva(RelationalInputGenerator input, ResultReceiver resultReceiver) throws AlgorithmConfigurationException {
        DVA dva = new DVA();
        dva.setRelationalInputConfigurationValue(DVA.Identifier.INPUT_GENERATOR.name(), input);
        dva.setIntegerConfigurationValue(DVA.Identifier.INPUT_ROW_LIMIT.name(), InputConfigurationSingleton.get().getFILE_MAX_ROWS());
        dva.setResultReceiver(resultReceiver);

        return dva;
    }

    public static HyUCC createHyUCC(RelationalInputGenerator input, ResultCache resultReceiver) throws AlgorithmConfigurationException {
        HyUCC hyUCC = new HyUCC();
        hyUCC.setRelationalInputConfigurationValue(HyUCC.Identifier.INPUT_GENERATOR.name(), input);
        hyUCC.setBooleanConfigurationValue(HyUCC.Identifier.NULL_EQUALS_NULL.name(), InputConfigurationSingleton.get().getFILE_NULL_EQUALS_NULL());
        hyUCC.setBooleanConfigurationValue(HyUCC.Identifier.VALIDATE_PARALLEL.name(), InputConfigurationSingleton.get().getVALIDATE_PARALLEL());
        hyUCC.setBooleanConfigurationValue(HyUCC.Identifier.ENABLE_MEMORY_GUARDIAN.name(), InputConfigurationSingleton.get().getENABLE_MEMORY_GUARDIAN());
        hyUCC.setIntegerConfigurationValue(HyUCC.Identifier.MAX_UCC_SIZE.name(), InputConfigurationSingleton.get().getMAX_SEARCH_SPACE_LEVEL());
        hyUCC.setIntegerConfigurationValue(HyUCC.Identifier.INPUT_ROW_LIMIT.name(), InputConfigurationSingleton.get().getFILE_MAX_ROWS());
        hyUCC.setResultReceiver(resultReceiver);
        return hyUCC;
    }

    public static HyFD createHyFD(RelationalInputGenerator input, ResultCache resultReceiver) throws AlgorithmConfigurationException {
        HyFD hyFD = new HyFD();
        hyFD.setRelationalInputConfigurationValue(HyFD.Identifier.INPUT_GENERATOR.name(), input);
        hyFD.setBooleanConfigurationValue(HyFD.Identifier.NULL_EQUALS_NULL.name(), InputConfigurationSingleton.get().getFILE_NULL_EQUALS_NULL());
        hyFD.setBooleanConfigurationValue(HyFD.Identifier.VALIDATE_PARALLEL.name(), InputConfigurationSingleton.get().getVALIDATE_PARALLEL());
        hyFD.setBooleanConfigurationValue(HyFD.Identifier.ENABLE_MEMORY_GUARDIAN.name(), InputConfigurationSingleton.get().getENABLE_MEMORY_GUARDIAN());
        hyFD.setIntegerConfigurationValue(HyFD.Identifier.MAX_DETERMINANT_SIZE.name(), InputConfigurationSingleton.get().getMAX_SEARCH_SPACE_LEVEL());
        hyFD.setResultReceiver(resultReceiver);
        return hyFD;
    }

    public static void writeResultsToFile(DependencyType type, String algo, String fileName, long time, List<Result> results) throws IOException {
        FileUtils.writeToFile(
                algo + "\r\n\r\n" +
                        "Runtime: " + time + "\r\n\r\n" +
                        "Results: " + results.size() + "\r\n\r\n" +
                        fileName,
                InputConfigurationSingleton.get().getFileStatisticPath(fileName, type.toString()));
        FileUtils.writeToFile(formatResults(type, results), InputConfigurationSingleton.get().getFileResultPath(fileName, type.toString()));
    }

    public static List<ColumnIdentifier> getAcceptedColumns(RelationalInputGenerator relationalInputGenerator) throws InputGenerationException, AlgorithmConfigurationException {
        List<ColumnIdentifier> acceptedColumns = new ArrayList<>();
        RelationalInput relationalInput = relationalInputGenerator.generateNewCopy();
        String tableName = relationalInput.relationName();
        for (String columnName : relationalInput.columnNames())
            acceptedColumns.add(new ColumnIdentifier(tableName, columnName));
        return acceptedColumns;
    }

    public static RelationalInputGenerator getInput(String fileName) throws AlgorithmConfigurationException {
        return InputConfigurationSingleton.get().getInputGenerator(fileName);
    }


    private static String formatResults(DependencyType type, List<Result> tempResults) {
        switch (type){
            case UCC:
                return formatUCC(tempResults);
            case FD:
                return formatFD(tempResults);
            case IND:
                return formatIND(tempResults);
            case CARD:
                return formatCard(tempResults);
            default:
                return null;
        }
    }

    private static String formatIND(List<Result> results) {
        HashMap<String, List<String>> lhs2rhs = new HashMap<>();

        for (Result result : results) {
            InclusionDependency ind = (InclusionDependency) result;
            StringBuilder lhsBuilder = new StringBuilder("[");
            Iterator<ColumnIdentifier> iterator = ind.getReferenced().getColumnIdentifiers().iterator();
            while (iterator.hasNext()) {
                lhsBuilder.append(iterator.next().toString());
                if (iterator.hasNext())
                    lhsBuilder.append(", ");
            }
            lhsBuilder.append("]");
            String lhs = lhsBuilder.toString();

            String rhs = ind.getDependant().toString();

            if (!lhs2rhs.containsKey(lhs))
                lhs2rhs.put(lhs, new ArrayList<>());
            lhs2rhs.get(lhs).add(rhs);
        }

        StringBuilder builder = new StringBuilder();
        //builder.append("\r\n").append("INDS:").append("\r\n");
        ArrayList<String> lhss = new ArrayList<>(lhs2rhs.keySet());
        Collections.sort(lhss);
        for (String lhs : lhss) {
            List<String> rhss = lhs2rhs.get(lhs);
            Collections.sort(rhss);

            if (rhss.isEmpty())
                continue;

            builder.append(lhs).append(" --> ");
            builder.append(CollectionUtils.concat(rhss, ", "));
            builder.append("\r\n");
        }
        return builder.toString();
    }

    private static String formatUCC(List<Result> results) {
        StringBuilder builder = new StringBuilder();
        //builder.append("\r\n").append("UCCS:").append("\r\n");
        for (Result result : results) {
            UniqueColumnCombination ucc = (UniqueColumnCombination) result;
            ColumnCombination columnCombination = ucc.getColumnCombination();
            builder.append("[").append(CollectionUtils.concat(columnCombination.getColumnIdentifiers(), ", ")).append("]").append("\r\n");
        }

        return builder.toString();
    }

    private static String formatCard(List<Result> results) {
        StringBuilder builder = new StringBuilder();
        for (Result result : results) {
            BasicStatistic card = (BasicStatistic) result;
            builder.append("[").append(card.getColumnCombination().toString()).append("]").append(card.getStatisticMap()).append("\r\n");
        }

        return builder.toString();
    }

    private static String formatFD(List<Result> results) {
        HashMap<String, List<String>> lhs2rhs = new HashMap<>();

        for (Result result : results) {
            FunctionalDependency fd = (FunctionalDependency) result;
            StringBuilder lhsBuilder = new StringBuilder("[");
            Iterator<ColumnIdentifier> iterator = fd.getDeterminant().getColumnIdentifiers().iterator();
            while (iterator.hasNext()) {
                lhsBuilder.append(iterator.next().toString());
                if (iterator.hasNext())
                    lhsBuilder.append(", ");
            }
            lhsBuilder.append("]");
            String lhs = lhsBuilder.toString();

            String rhs = fd.getDependant().toString();

            if (!lhs2rhs.containsKey(lhs))
                lhs2rhs.put(lhs, new ArrayList<>());
            lhs2rhs.get(lhs).add(rhs);
        }

        StringBuilder builder = new StringBuilder();
        ArrayList<String> lhss = new ArrayList<>(lhs2rhs.keySet());
        Collections.sort(lhss);
        for (String lhs : lhss) {
            List<String> rhss = lhs2rhs.get(lhs);
            Collections.sort(rhss);

            if (rhss.isEmpty())
                continue;

            builder.append(lhs).append(" --> ");
            builder.append(CollectionUtils.concat(rhss, ", "));
            builder.append("\r\n");
        }
        return builder.toString();
    }


    public static List<Result> getResultType(DependencyType type, String depString){
        List<Result> results = new ArrayList<>();
        switch (type){
            case UCC:
                ColumnCombination temp = new ColumnCombination();
                temp.setColumnIdentifiers(parseCombination(depString));
                results.add(new UniqueColumnCombination(temp));
                break;
            case FD:
                String[] split = depString.split(" --> ");
                ColumnCombination left = new ColumnCombination();
                if(split[0].equalsIgnoreCase("[]"))
                    break;
                left.setColumnIdentifiers(parseCombination(split[0]));
                for(String right : split[1].split(",")){
                    ColumnIdentifier rightID = parseIdentifier(right.trim());
                    results.add(new FunctionalDependency(left, rightID));
                }
                break;
            case IND:
                split = depString.split(" --> ");
                ColumnPermutation right = new ColumnPermutation();
                right.setColumnIdentifiers(parsePermutation(split[0]));
                List<List<ColumnIdentifier>> permutations = parsePermutations(split[1]);
                for (List<ColumnIdentifier> p : permutations) {
                    ColumnPermutation leftIND = new ColumnPermutation();
                    leftIND.setColumnIdentifiers(p);
                    ColumnPermutation rightClone = new ColumnPermutation();
                    rightClone.setColumnIdentifiers(new ArrayList<>(right.getColumnIdentifiers()));
                    results.add(new InclusionDependency(leftIND, rightClone));
                }
                break;
            case CARD:
                String name = depString.substring(depString.indexOf("[[") + 2, depString.indexOf("]]"));
                String values = depString.substring(depString.indexOf("=") + 1, depString.indexOf("}"));
                BasicStatistic bs = new BasicStatistic(parseIdentifier(name));
                bs.addStatistic("Number of Distinct Values", new BasicStatisticValueLong(Long.parseLong(values)));
                results.add(bs);
                break;
            default:
                break;
        }
        return results;
    }

    private static List<List<ColumnIdentifier>> parsePermutations(String input) {
        List<List<ColumnIdentifier>> allResults = new ArrayList<>();
        while(input.contains("]")){
            int start = input.indexOf("[");
            int end = input.indexOf("]"); // get index of second bracket
            String output;
            if(end+1 >  input.length())
                output = input.substring(start, end + 1);
            else
                output = input.substring(start, end+1);
            input = input.substring(end + 1);
            allResults.add(parsePermutation(output));
        }
        return allResults;
    }

    /**
     * @TODO Check that multiple INDS are given back as multiple and not combined to a single
     * e.g. [REFERENCED.csv."Key_NON_UCC_1"] --> [DEPENDANT.csv."Key_NON_UCC_1"], [DEPENDANT.csv."Key_UCC_1"]
     * VS.  [REFERENCED.csv."Key_NON_UCC_1", REFERENCED.csv."Key_NON_UCC_1"] --> [DEPENDANT.csv."Key_NON_UCC_1", DEPENDANT.csv."Key_UCC_1"]
     * @param input
     * @return
     */
    private static List<ColumnIdentifier> parsePermutation(String input) {
        List<ColumnIdentifier> result = new ArrayList<>();
        while(input.contains("]")){
            int start = 0;
            if(input.contains("[")){
                start = input.indexOf("["); // get index of first bracket
            }
            int end = -1;
            if(input.contains(",")){
                end = input.indexOf(","); // get index of first bracket
            } else {
                end = input.indexOf("]"); // get index of second bracket
            }
            String output = input.substring(start + 1, end); // get text between brackets
            input = input.substring(end + 1);
            result.add(parseIdentifier(output));
        }
        return result;
    }

    private static Set<ColumnIdentifier> parseCombination(String input) {
        Set<ColumnIdentifier> result = new HashSet<>();
        while(input.contains("]")){
            int start = 0;
            if(input.contains("[")){
                start = input.indexOf("["); // get index of first bracket
            }
            int end = -1;
            if(input.contains(",")){
                end = input.indexOf(","); // get index of first bracket
            } else {
                end = input.indexOf("]"); // get index of second bracket
            }
            String output = input.substring(start + 1, end); // get text between brackets
            input = input.substring(end + 1);
            result.add(parseIdentifier(output));
        }
        return result;
    }

    private static ColumnIdentifier parseIdentifier(String input) {
        String[] tableCol = input.split("\\."+InputConfigurationSingleton.get().getFILE_ENDING()); // NATION.csv."N_REGIONKEY"
        ColumnIdentifier id = new ColumnIdentifier();

        id.setColumnIdentifier(tableCol[1].replace(".", ""));
        id.setTableIdentifier(tableCol[0] + "." + InputConfigurationSingleton.get().getFILE_ENDING());

        return id;
    }

}
