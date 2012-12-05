package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import no.uib.cipr.matrix.sparse.SparseVector;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;

/**
 * Accepts only rows with non-zero coefficients whose absolute values are greater than some value.
 */
public class NumericalStabilityRltRowFilter implements RltRowFilter {

    private double minCoef;
    private double maxCoef;
    
    public NumericalStabilityRltRowFilter(double minCoef, double maxCoef) {
        this.minCoef = minCoef;
        this.maxCoef = maxCoef;
    }
    
    @Override
    public void init(Rlt rlt) throws IloException {
        // Do nothing.
    }
    
    @Override
    public boolean acceptEq(SparseVector row, String rowName, Factor facI, int k) {
        return accept(row);
    }

    @Override
    public boolean acceptLeq(SparseVector row, String rowName, Factor facI, Factor facJ) {
        return accept(row);
    }

    private boolean accept(SparseVector row) {
        double[] data = row.getData();
        for (int i=0; i<row.getUsed(); i++) {
            double absVal = Math.abs(data[i]);
            if (absVal < minCoef || maxCoef < absVal) {
                return false;
            }
        }
        return true;
    }
}