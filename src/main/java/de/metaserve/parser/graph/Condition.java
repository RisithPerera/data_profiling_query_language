package de.metaserve.parser.graph;

import de.metaserve.executor.min.constraints.CardinalityConstraint;
import de.metaserve.executor.min.constraints.Interval;
import de.metaserve.executor.min.constraints.SizeConstraint;

import java.util.List;
import java.util.Map;

public interface Condition {

    static Condition build(String condition, Map<String, List<String>> ccFunctions) {
        boolean not = false;
        if(condition.startsWith("NOT")){
            condition = condition.replace("NOT", "").trim();
            not = true;
        }
        String idDEP = condition.substring(0,3);
        switch (idDEP){
            case IND.NAME:
                String[] ids_IND = getIDS(condition);
                String ref = ids_IND[0].trim();
                String dep = ids_IND[1].trim();
                if(ref.equals(dep))
                    throw new RuntimeException("Sources can not be the same in: " + IND.NAME + "(" + dep +","+ref + ")");
                return new IND(ccFunctions.get(ref), ccFunctions.get(dep), ref, dep);
            case FD.NAME+"(":
                String[] ids_FD = getIDS(condition);
                String left = ids_FD[0].trim();
                String right = ids_FD[1].trim();
                if(left.equals(right))
                    throw new RuntimeException("Sources can not be the same in: " + FD.NAME + "(" + left +","+ right + ")");
                return new FD(ccFunctions.get(left), ccFunctions.get(right), left, right);
            case UCC.NAME:
                String id = getID(condition).trim();
                return new UCC(ccFunctions.get(id), id, not);
            case "SPL": //SPLIT
                String[] ids_split = getIDS(condition);
                String left_split = ids_split[0].trim();
                String right_split = ids_split[1].trim();
                //Config.split = true; @TODO
                return new Split(left_split, right_split);
            case "CON":
                String[] ids_con = getIDS(condition);
                String left_con = ids_con[0].trim();
                String right_con = ids_con[1].trim();
                return new Contains(left_con, right_con);
            case "COA":
                String[] ids_coa = getIDS(condition);
                String left_coa = ids_coa[0].trim();
                String right_coa = ids_coa[1].trim();
                return new Coalesce(left_coa, right_coa);
            case "SIZ": //SIZE @TODO
//                String id_size = getID(condition).trim();
//                int maxSize = getNumberFromCondition(condition);
//                InputConfigurationSingleton.get().setMAX_SEARCH_SPACE_LEVEL(maxSize);
//                if(getComparision(condition) > 0)
//                    throw new RuntimeException("Size is currently only supported to minimize a result set!");
//                break;
                String id_size = getID(condition).trim();
                int sizeValue = getNumberFromCondition(condition);
                int sizeComparison = getComparision(condition);
                Interval sizeInterval = buildInterval(sizeComparison, sizeValue);
                return new SizeConstraint(id_size, sizeInterval);
            case "CAR": //CARD
                String id_card = getID(condition).trim();
//                int size = getNumberFromCondition(condition);
//                int cp = getComparision(condition);
//                if(cp > 0)
//                    InputConfiguration.minCard = size;
//                else if(cp < 0)
//                    InputConfiguration.maxCard = size;
//                else {
//                    InputConfiguration.minCard = size-1;
//                    InputConfiguration.maxCard = size+1;
//                }
//                InputConfigurationSingleton.get().buildCardMap(ccFunctions.get(id_card));
                int cardValue = getNumberFromCondition(condition);
                int cardComparison = getComparision(condition);
                Interval cardInterval = buildInterval(cardComparison, cardValue);
                return new CardinalityConstraint(id_card, cardInterval);
            default:
                break;
        }
        return null;
    }

    static Interval buildInterval(int comparision, int value){
        int min = 1;
        int max = Integer.MAX_VALUE;
        switch (comparision){
            case 2: // >
                min = value + 1;
                break;
            case 1: // >=
                min = value;
                break;
            case -2: // <
                max = value - 1;
                break;
            case -1: // <=
                max = value;
            case 0: //=
            case -3: //missing
                min = value;
                max = value;
            default:
                break;
        }
        if (min < 1)
            min = 1;
        if (max < min)
            throw new IllegalArgumentException("Invalid interval bounds computed for condition value " + value);
        return Interval.of(min, max);
    }


    static int getComparision(String condition) {
        if(condition.contains("<") && condition.contains("="))
            return -1;
        else if(condition.contains("<"))
            return -2;
        else if(condition.contains(">") && condition.contains("="))
            return 1;
        else if(condition.contains(">"))
            return 2;
        else if(condition.contains("="))
            return 0;
        else
            return -3;
    }

    static int getNumberFromCondition(String condition) {
        return Integer.parseInt(condition.replaceAll("[^\\d]", "").trim());
    }

    static String getID(String condition) {
        return condition.split("\\(")[1].split("\\)")[0];
    }

    static  String[] getIDS(String condition) {
        return condition.split("\\(")[1].split("\\)")[0].split(",");
    }


    String getName();
}
