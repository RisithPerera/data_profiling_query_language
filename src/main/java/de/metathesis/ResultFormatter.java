package de.metathesis;

import de.metanome.algorithm_integration.ColumnCombination;
import de.metanome.algorithm_integration.ColumnIdentifier;
import de.metanome.algorithm_integration.ColumnPermutation;
import de.metanome.algorithm_integration.results.InclusionDependency;
import de.metanome.algorithm_integration.results.MultivaluedDependency;
import de.metanome.algorithm_integration.results.UniqueColumnCombination;
import de.metathesis.structures.AttributeBitSet;
import de.metathesis.structures.results.FDResult;
import de.metathesis.structures.results.INDResult;
import de.metathesis.structures.results.UCCResult;

import java.util.BitSet;

public class ResultFormatter {
    private static final ResultFormatter INSTANCE = new ResultFormatter();

    private final Preprocessor preprocessor;

    private ResultFormatter() {
        this.preprocessor = Preprocessor.getInstance();
    }

    public static ResultFormatter getInstance() {
        return INSTANCE;
    }

    private String format(AttributeBitSet abs) {

        String relName = this.preprocessor.getRelation(abs.getRelationIndex()).getName();
        String[] cols = this.preprocessor.getRelation(abs.getRelationIndex()).getAttributeNames();

        StringBuilder sb = new StringBuilder();
        sb.append(relName).append("(");

        BitSet bs = abs.getAttributeIndexSet();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            sb.append(cols[i]).append(';');
        }
        sb.setLength(sb.length() - 1);
        sb.append(")");
        return sb.toString();
    }

    public void printAttributeSet(AttributeBitSet... sets) {
        for (int i = 0; i < sets.length; i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(format(sets[i]));
        }
        System.out.println();
    }

    public ColumnIdentifier[] formatAbs(AttributeBitSet abs) {
        String relName = this.preprocessor.getRelation(abs.getRelationIndex()).getName();
        String[] cols = this.preprocessor.getRelation(abs.getRelationIndex()).getAttributeNames();

        ColumnIdentifier[] cdList = new ColumnIdentifier[abs.size()];

        BitSet bs = abs.getAttributeIndexSet();
        int outIdx = 0;

        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            cdList[outIdx++] = new ColumnIdentifier(relName, cols[i]);
        }

        return cdList;
    }

    public UniqueColumnCombination formatUCC(UCCResult.UCC ucc) {
        return new UniqueColumnCombination(formatAbs(ucc.lhs));
    }

    public InclusionDependency formatIND(INDResult.IND ind) {
        ColumnPermutation dependant = new ColumnPermutation(formatAbs(ind.lhs));
        ColumnPermutation referenced = new ColumnPermutation(formatAbs(ind.rhs));
        return new InclusionDependency(dependant, referenced);
    }

    public MultivaluedDependency formatFD(FDResult.FD fd) {
        ColumnCombination determinant = new ColumnCombination(formatAbs(fd.lhs));
        ColumnCombination dependant = new ColumnCombination(formatAbs(fd.rhs));
        return new MultivaluedDependency(determinant, dependant);
    }

    public void printUCC(UCCResult result) {
        result.forEach(ucc -> System.out.println(format(ucc.lhs)));
    }

    public void printIND(INDResult result) {
        result.forEach(ind -> System.out.println(format(ind.lhs) + " ⊆ " + format(ind.rhs)));
    }

    public void printFD(FDResult result) {
        result.forEach(fd -> System.out.println(format(fd.lhs) + " → " + format(fd.rhs)));
    }
}
