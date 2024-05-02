package de.metaserve.model.constraints;

import de.metaserve.util.configuration.InputConfiguration;
import de.metaserve.util.singletons.InputConfigurationSingleton;

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
                break;
            case "SIZ": //SIZE @TODO
                String id_size = getID(condition).trim();
                int maxSize = getNumberFromCondition(condition);
                InputConfigurationSingleton.get().setMAX_SEARCH_SPACE_LEVEL(maxSize);
                if(getComparision(condition) > 0)
                    throw new RuntimeException("Size is currently only supported to minimize a result set!");
                break;
            case "CAR": //CARD
                String id_card = getID(condition).trim();
                int size = getNumberFromCondition(condition);
                int cp = getComparision(condition);
                if(cp > 0)
                    InputConfiguration.minCard = size;
                else if(cp < 0)
                    InputConfiguration.maxCard = size;
                else {
                    InputConfiguration.minCard = size-1;
                    InputConfiguration.maxCard = size+1;
                }
                InputConfigurationSingleton.get().buildCardMap(ccFunctions.get(id_card));
                break;
            default:
                break;
        }
        return null;
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
