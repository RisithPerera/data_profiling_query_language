package de.metaserve.model.dpal;

import lombok.Data;

@Data
public class GraphMetadata {
    private int FD_Decomposition = 0;
    private int Bushy_Decomposition = 0;
    private int IND_Decomposition = 0;
    private int UCC_sub_extending_UCC_Rule = 0;
    private int FD_extending_UCC_Rule = 0;
    private int FD_sub_extending_UCC_Rule = 0;
    private int FD_fork_with_FD_Rule = 0;
    private int FD_chained_to_FD_Rule = 0;
    private int FD_chained_to_UCC_Rule = 0;
    private int IND_with_UCC_Rule = 0;
    private int IND_with_FD_Rule = 0;
    private int UCC_extending_UCC_Rule=0;

    public void add(GraphMetadata metadata) {
        FD_Decomposition += metadata.getFD_Decomposition();
        Bushy_Decomposition += metadata.getBushy_Decomposition();
        IND_Decomposition += metadata.getIND_Decomposition();
        UCC_sub_extending_UCC_Rule += metadata.getUCC_sub_extending_UCC_Rule();
        UCC_extending_UCC_Rule += metadata.getUCC_extending_UCC_Rule();
        FD_extending_UCC_Rule += metadata.getFD_extending_UCC_Rule();
        FD_sub_extending_UCC_Rule += metadata.getFD_sub_extending_UCC_Rule();
        FD_fork_with_FD_Rule += metadata.getFD_fork_with_FD_Rule();
        FD_chained_to_FD_Rule += metadata.getFD_chained_to_FD_Rule();
        FD_chained_to_UCC_Rule += metadata.getFD_chained_to_UCC_Rule();
        IND_with_UCC_Rule += metadata.getIND_with_UCC_Rule();
        IND_with_FD_Rule += metadata.getIND_with_FD_Rule();
    }

    public void increaseFDDecomposition() {
        FD_Decomposition++;
    }

    public void increaseBushyDecomposition() {
        Bushy_Decomposition++;
    }

    public void increaseINDDecomposition() {
        IND_Decomposition++;
    }

    public void increaseUccExtendingUccRule() {
        UCC_extending_UCC_Rule++;
    }

    public void increaseUccSubExtendingUccRule() {
        UCC_sub_extending_UCC_Rule++;
    }

    public void increaseFdExtendingUccRule() {
        FD_extending_UCC_Rule++;
    }

    public void increaseFdSubExtendingUccRule() {
        FD_sub_extending_UCC_Rule++;
    }

    public void increaseFdForkWithFdRule() {
        FD_fork_with_FD_Rule++;
    }

    public void increaseFdChainedToFDRule() {
        FD_chained_to_FD_Rule++;
    }

    public void increaseFdChainedToUccRule() {
        FD_chained_to_UCC_Rule++;
    }

    public void increaseIndWithFdRule() {
        IND_with_UCC_Rule++;
    }
    public void increaseIndWithUccRule() {
        IND_with_FD_Rule++;
    }

}
