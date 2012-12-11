package edu.jhu.hltcoe.gridsearch.dmv;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.RelaxStatus;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;

public class RelaxedDmvSolution implements RelaxedSolution {

    private static final Logger log = Logger.getLogger(RelaxedDmvSolution.class);

    private double score;
    private double[][] logProbs;
    private RelaxStatus status;
    private RelaxedDepTreebank treebank;
    private double[][] featCounts;
    private double[][] objVals;
    private double trueRelaxObj;

    public RelaxedDmvSolution(double[][] logProbs, RelaxedDepTreebank treebank, double score, RelaxStatus status,
            double[][] featCounts, double[][] objVals, double trueRelaxObj) {
        super();
        this.score = score;
        this.logProbs = logProbs;
        this.treebank = treebank;
        this.status = status;
        this.featCounts = featCounts;
        this.objVals = objVals;
        this.trueRelaxObj = trueRelaxObj;
    }

    @Override
    public double getScore() {
        return score;
    }

    public double[][] getLogProbs() {
        return logProbs;
    }

    public RelaxStatus getStatus() {
        return status;
    }

    public RelaxedDepTreebank getTreebank() {
        return treebank;
    }

    public double[][] getFeatCounts() {
        return featCounts;
    }

    public double[][] getObjVals() {
        return objVals;
    }

    /**
     * Get the true quadratic objective given the model parameters and feature
     * counts found by the relaxation.
     */
    public double getTrueObjectiveForRelaxedSolution() {
        return trueRelaxObj;
    }

    public void setScore(double optimisticBound) {
        score = optimisticBound;
    }

}
