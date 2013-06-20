/**
 * 
 */
package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.lp.FactorBuilder;
import edu.jhu.hltcoe.lp.FactorBuilder.Factor;

public interface RltFactorFilter {
    boolean accept(Factor factor);
    void init(Rlt rlt) throws IloException;
}