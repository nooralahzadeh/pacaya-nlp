package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.Projector;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProjector.DmvProjectorPrm;
import edu.jhu.hltcoe.model.dmv.CopyingDmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.ViterbiTrainer;
import edu.jhu.hltcoe.train.ViterbiTrainer.ViterbiTrainerPrm;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

public class ViterbiEmDmvProjector implements Projector {

    public static class ViterbiEmDmvProjectorPrm {
        public double proportionViterbiImproveTreebank = 0.05;
        public double proportionViterbiImproveModel = 0.05;
        public DmvProjectorPrm projPrm = new DmvProjectorPrm();
    }
    
    private final class DmvSolutionComparator implements Comparator<DmvSolution> {
        /**
         * This will only return nulls if there are no non-null entries
         */
        @Override
        public int compare(DmvSolution sol1, DmvSolution sol2) {
            if (sol1 == null && sol2 == null) {
                return 0;
            } else if (sol1 == null) {
                return 1;
            } else if (sol2 == null) {
                return -1;
            } else {
                return Double.compare(sol1.getScore(), sol2.getScore());
            }
        }
    }

    private static final Logger log = Logger.getLogger(ViterbiEmDmvProjector.class);

    private ViterbiEmDmvProjectorPrm prm;
    private DmvProjector dmvProjector;
    private DmvTrainCorpus corpus;
    private DmvRelaxation dwRelax;
    private DmvSolution initFeasSol;

    public ViterbiEmDmvProjector(ViterbiEmDmvProjectorPrm prm, DmvTrainCorpus corpus, DmvRelaxation dwRelax, DmvSolution initFeasSol) {
        this.prm = prm;
        dmvProjector = new DmvProjector(prm.projPrm, corpus);
        this.corpus = corpus;
        this.dwRelax = dwRelax;
        this.initFeasSol = initFeasSol;
    }
    
    @Override
    public Solution getProjectedSolution(RelaxedSolution relaxSol) {
        return getProjectedDmvSolution((RelaxedDmvSolution) relaxSol);
    }

    public DmvSolution getProjectedDmvSolution(RelaxedDmvSolution relaxSol) {
        if (relaxSol == null) {
            throw new IllegalStateException("No relaxed solution cached.");
        }
        List<DmvSolution> solutions = new ArrayList<DmvSolution>();
        if (initFeasSol != null) {
            // Only consider this solution once at the root.
            solutions.add(initFeasSol);
            initFeasSol = null;
        }
        
        DmvSolution projectedSol = dmvProjector.getProjectedDmvSolution(relaxSol);
        // Add the null solution, so that the collection isn't empty.
        solutions.add(projectedSol);
        if (projectedSol != null) {
            // TODO: These solutions might not be feasible according to the
            // root bounds.
            // TODO: Decide on a better heuristic for when to do this 
            // (e.g. depth > dwRelax.getIdm().getNumTotalParams())

            // Run Viterbi EM starting from the randomly rounded solution.
            if (Prng.nextDouble() < prm.proportionViterbiImproveTreebank) {
                solutions.add(getImprovedSol(projectedSol.getTreebank()));
            }
            if (Prng.nextDouble() < prm.proportionViterbiImproveModel) {
                solutions.add(getImprovedSol(projectedSol.getLogProbs(), projectedSol.getIdm()));
            }
        }

        return Collections.max(solutions, new DmvSolutionComparator());
    }
    

    private DmvSolution getImprovedSol(double[][] logProbs, IndexedDmvModel idm) {
        double lambda = 1e-6;
        // TODO: this is a slow conversion
        DmvModel model = idm.getDmvModel(logProbs);
        // We must smooth the weights so that there exists some valid parse
        model.backoff(Utilities.log(lambda));
        model.logNormalize();
        DmvModelFactory modelFactory = new CopyingDmvModelFactory(model);
        return runViterbiEmHelper(modelFactory, 0);
    }
    
    private DmvSolution getImprovedSol(DepTreebank treebank) {  
        double lambda = 0.1;
        // Do one M-step to create a model
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModel model = (DmvModel) mStep.getModel(corpus, treebank);
        DmvModelFactory modelFactory = new CopyingDmvModelFactory(model);
        // Then run Viterbi EM
        return runViterbiEmHelper(modelFactory, 0);
    }

    private DmvSolution runViterbiEmHelper(DmvModelFactory modelFactory, 
            int numRestarts) {
        // Run Viterbi EM to improve the projected solution.
        ViterbiParser parser = new DmvCkyParser();
        ViterbiTrainerPrm prm = new ViterbiTrainerPrm();
        prm.emPrm.iterations = 25;        
        prm.emPrm.convergenceRatio = 0.99999;
        prm.emPrm.numRestarts = numRestarts;
        prm.emPrm.timeoutSeconds = Double.NEGATIVE_INFINITY;
        prm.lambda = 0.1;
        prm.evaluator = null;
        ViterbiTrainer trainer = new ViterbiTrainer(prm, parser, modelFactory);
        trainer.train(corpus);
        
        DepTreebank treebank = trainer.getCounts();
        IndexedDmvModel idm = dwRelax.getIdm();
        DmvModel dmv = (DmvModel)trainer.getModel();
        double[][] logProbs = idm.getCmLogProbs(dmv);
        
        // Compute the score for the solution
        double score = dwRelax.computeTrueObjective(logProbs, treebank);
        log.debug("Computed true objective: " + score);
        assert Utilities.equals(score, trainer.getLogLikelihood(), 1e-5) : "difference = " + (score - trainer.getLogLikelihood());
                
        // We let the DmvProblemNode compute the score
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, score);
        return sol;
    }

}
