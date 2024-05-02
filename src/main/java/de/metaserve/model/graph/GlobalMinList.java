package de.metaserve.model.graph;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalMinList {
    Set<ColumnCombination> allowedCCs = new HashSet<>();
    boolean first = true;
    boolean empty = false;

    public GlobalMinList(){

    }

    public void add(Collection<ColumnCombination> ccs){
        if(empty)
            return;
        if(first) {
            allowedCCs.addAll(ccs);
            first = false;
            return;
        }
        Set<ColumnCombination> ciSet = new HashSet<>(ccs);
        allowedCCs.retainAll(ciSet);
        if(allowedCCs.isEmpty())
            empty = true;
    }

    public Set<ColumnCombination> getAllowedCCs(){
        return allowedCCs;
    }

    public void apply(NewConstraint constraint) {
        allowedCCs = constraint.apply(allowedCCs);
    }
}
