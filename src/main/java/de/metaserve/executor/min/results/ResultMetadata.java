package de.metaserve.executor.min.results;

import de.metaserve.executor.min.graph.edge.Edge;
import de.metaserve.executor.min.graph.Graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ResultMetadata {
    int f = 0;
    int fPlus = 0;
    int fValid = 0;
    int u = 0;
    int uPlus = 0;
    int i = 0;
    int iPlus = 0;
    int iMinus = 0;
    int exists = 0;

    int hard1 = 0;
    int hard2 = 0;
    int hard3 = 0;
    int hard4 = 0;
    int hard5 = 0;

    public void add(Set<String> results) {
        Set<Set<String>> potentialIPlusOne = new HashSet<>();
        Set<String> iPlusNodes = new HashSet<>();
        boolean existsLocal = false;
        boolean existsMin = false;
        boolean existsValid = false;
        for (String result : results){
            if(result.contains("valid(")){
                fValid++;
                existsValid = true;
            } else if(result.endsWith("F^+")) {
                fPlus++;
            } else if(result.endsWith("F")) {
                existsMin = true;
                f++;
            } else if(result.endsWith("U^+")) {
                uPlus++;
            } else if(result.endsWith("U")) {
                existsMin = true;
                u++;
            } else if(result.contains("| = 1")) {
                existsMin = true;
                int openIndex = result.indexOf('|');
                String charAtOpen = String.valueOf(result.charAt(openIndex+1));
                //System.out.println(charAtOpen);
                iPlusNodes.add(charAtOpen);
            } else if(result.contains("I^+")) {
                int openParenIndex = result.indexOf('(');
                int closeParenIndex = result.indexOf(')');
                String beforeOpenParen = String.valueOf(result.charAt(openParenIndex+1));
                String beforeCloseParen = String.valueOf(result.charAt(closeParenIndex-1));
                //System.out.println(beforeOpenParen + " " + beforeCloseParen);
                Set<String> canidateSet = new HashSet<>();
                canidateSet.add(beforeOpenParen);
                canidateSet.add(beforeCloseParen);
                potentialIPlusOne.add(canidateSet);
                iPlus++;
            } else if(result.contains("I")) {
                i++;
            } else if (result.contains("\\subset ")) {
                existsLocal = true;
            } else if (result.contains(" \\rightarrow ") || result.contains(" \\subseteq ") || result.contains("(")){
                //System.out.println(result);
            } else {
                throw new RuntimeException("Not implemented yet! " + result);
            }
        }
        for (Set<String> potential : potentialIPlusOne){
            if (isPlusOne(potential, iPlusNodes)) {
                iMinus++;
                iPlus--;
            }
        }
        if (existsLocal)
            exists++;

        if (existsMin && (!existsValid) && (!existsLocal)){
            hard1++;
        } else if (existsMin && !existsValid){
            hard2++;
        } else if (existsMin){
            hard3++;
        } else if (!existsValid && existsLocal){
            hard4++;
        } else if (existsValid && existsLocal){
            hard5++;
        } else {
            //System.out.println("Missed Combo: " + existsMin + " " + existsValid + " " + existsLocal + " " + results);
            hard5++;
        }
    }

    private boolean isPlusOne(Set<String> set, Set<String> nodes){
        for (String node : nodes){
            if (set.contains(node))
                return true;
        }
        return false;
    }

    public void add(HashMap<Edge, Graph.SetMembership> setMembershipMap) {
        for (Graph.SetMembership setMembership : setMembershipMap.values()){
            switch (setMembership){
                case F:
                    f++;
                    break;
                case F_PLUS:
                    fPlus++;
                    break;
                case F_PLUS_VALID:
                    fValid++;
                    break;
                case U:
                    u++;
                    break;
                case U_PLUS:
                    uPlus++;
                    break;
                case I:
                    i++;
                    break;
                case I_MINUS:
                    iMinus++;
                    break;
                case I_PLUS:
                    iPlus++;
                    break;
            }
        }
    }

    public int getF() {
        return f;
    }

    public int getfPlus() {
        return fPlus;
    }

    public int getfValid() {
        return fValid;
    }

    public int getU() {
        return u;
    }

    public int getuPlus() {
        return uPlus;
    }

    public int getI() {
        return i;
    }

    public int getiPlus() {
        return iPlus;
    }

    public int getiMinus() {
        return iMinus;
    }

    public int getExists() {
        return exists;
    }

    public int getHard1() {
        return hard1;
    }

    public int getHard2() {
        return hard2;
    }

    public int getHard3() {
        return hard3;
    }

    public int getHard4() {
        return hard4;
    }

    public int getHard5() {
        return hard5;
    }
}
