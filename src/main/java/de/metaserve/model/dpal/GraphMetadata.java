package de.metaserve.model.dpal;

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

    public int getFD_Decomposition() {
        return FD_Decomposition;
    }

    public void setFD_Decomposition(int FD_Decomposition) {
        this.FD_Decomposition = FD_Decomposition;
    }

    public int getBushy_Decomposition() {
        return Bushy_Decomposition;
    }

    public void setBushy_Decomposition(int bushy_Decomposition) {
        Bushy_Decomposition = bushy_Decomposition;
    }

    public int getIND_Decomposition() {
        return IND_Decomposition;
    }

    public void setIND_Decomposition(int IND_Decomposition) {
        this.IND_Decomposition = IND_Decomposition;
    }

    public int getUCC_sub_extending_UCC_Rule() {
        return UCC_sub_extending_UCC_Rule;
    }

    public void setUCC_sub_extending_UCC_Rule(int UCC_sub_extending_UCC_Rule) {
        this.UCC_sub_extending_UCC_Rule = UCC_sub_extending_UCC_Rule;
    }

    public int getFD_extending_UCC_Rule() {
        return FD_extending_UCC_Rule;
    }

    public void setFD_extending_UCC_Rule(int FD_extending_UCC_Rule) {
        this.FD_extending_UCC_Rule = FD_extending_UCC_Rule;
    }

    public int getFD_sub_extending_UCC_Rule() {
        return FD_sub_extending_UCC_Rule;
    }

    public void setFD_sub_extending_UCC_Rule(int FD_sub_extending_UCC_Rule) {
        this.FD_sub_extending_UCC_Rule = FD_sub_extending_UCC_Rule;
    }

    public int getFD_fork_with_FD_Rule() {
        return FD_fork_with_FD_Rule;
    }

    public void setFD_fork_with_FD_Rule(int FD_fork_with_FD_Rule) {
        this.FD_fork_with_FD_Rule = FD_fork_with_FD_Rule;
    }

    public int getFD_chained_to_FD_Rule() {
        return FD_chained_to_FD_Rule;
    }

    public void setFD_chained_to_FD_Rule(int FD_chained_to_FD_Rule) {
        this.FD_chained_to_FD_Rule = FD_chained_to_FD_Rule;
    }

    public int getFD_chained_to_UCC_Rule() {
        return FD_chained_to_UCC_Rule;
    }

    public void setFD_chained_to_UCC_Rule(int FD_chained_to_UCC_Rule) {
        this.FD_chained_to_UCC_Rule = FD_chained_to_UCC_Rule;
    }

    public int getIND_with_UCC_Rule() {
        return IND_with_UCC_Rule;
    }

    public void setIND_with_UCC_Rule(int IND_with_UCC_Rule) {
        this.IND_with_UCC_Rule = IND_with_UCC_Rule;
    }

    public int getIND_with_FD_Rule() {
        return IND_with_FD_Rule;
    }

    public void setIND_with_FD_Rule(int IND_with_FD_Rule) {
        this.IND_with_FD_Rule = IND_with_FD_Rule;
    }

    public int getUCC_extending_UCC_Rule() {
        return UCC_extending_UCC_Rule;
    }

    public void setUCC_extending_UCC_Rule(int UCC_extending_UCC_Rule) {
        this.UCC_extending_UCC_Rule = UCC_extending_UCC_Rule;
    }
}
