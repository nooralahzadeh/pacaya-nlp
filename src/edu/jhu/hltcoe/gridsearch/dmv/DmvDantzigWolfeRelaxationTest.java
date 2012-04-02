package edu.jhu.hltcoe.gridsearch.dmv;

import static org.junit.Assert.assertEquals;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Dir;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.CutCountComputer;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.train.ViterbiTrainer;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;


public class DmvDantzigWolfeRelaxationTest {

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testQuadraticObjectiveInCplex() throws IloException {
        System.out.println("Trying to solve quadratic");
        IloCplex cplex = new IloCplex();
        
        IloNumVar thetaVar = cplex.numVar(-20, 0, "theta");
        IloNumVar edgeVar = cplex.numVar(0, 1, "edge");
        
        cplex.addMinimize(cplex.prod(edgeVar, thetaVar), "obj");
        
        cplex.exportModel(new File("quad.lp").getAbsolutePath());
        
        try {
            cplex.solve();
            Assert.fail();
        } catch (Exception e) {
            // pass
        }
    }
    
    @Test
    public void testOneWordSentence() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);
        
        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            for (int m=0; m<logProbs[c].length; m++) {
                
            }
        }
    }
    
    @Test
    public void testThreeWordSentence() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V P");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            for (int m=0; m<logProbs[c].length; m++) {
                
            }
        }
    }
    
    @Test
    public void testTwoSentences() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
        }
    }

    @Test
    public void testCutsOnManyPosTags() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");
        sentences.addSentenceFromString("Adj N a b c d e f g");
        sentences.addSentenceFromString("Adj N a c d f e b g");
        sentences.addSentenceFromString("Adj N a c d f e b g");
        sentences.addSentenceFromString("Adj N g f e d c b a");

        DmvDantzigWolfeRelaxation dw = getDw(sentences, 15);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            Assert.assertTrue(Vectors.sum(logProbs[c]) <= DmvDantzigWolfeRelaxation.MIN_SUM_FOR_CUT);
        }
    }
    
    
    @Test
    public void testBounds() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");
        //sentences.addSentenceFromString("N V");
        //sentences.addSentenceFromString("N V N N");
        //sentences.addSentenceFromString("D N");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);
        
        DmvBounds bds = dw.getBounds();
        double origLower = bds.getLb(0, 0);
        double origUpper = bds.getUb(0, 0);
        
        double newL, newU;

        newL = Utilities.log(0.11);
        newU = Utilities.log(0.90);
        
        RelaxedDmvSolution relaxSol;
        
        relaxSol = testBoundsHelper(dw, newL, newU, true);
        assertEquals(-1.4750472192095685, relaxSol.getScore(), 1e-13);

        newL = origLower;
        newU = origUpper;
        relaxSol = testBoundsHelper(dw, newL, newU, true);
        assertEquals(0.0, relaxSol.getScore(), 1e-13);
        
        assertEquals(origLower, bds.getLb(0, 0), 1e-7);
        assertEquals(origUpper, bds.getUb(0, 0), 1e-13);
        
    }

    private RelaxedDmvSolution testBoundsHelper(DmvDantzigWolfeRelaxation dw, double newL, double newU, boolean forward) {
        
        adjustBounds(dw, newL, newU, forward);
        
        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 

        System.out.println("Printing probabilities");
        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            double[] probs = Vectors.getExp(logProbs[c]);
            //System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            for (int m=0; m<logProbs[c].length; m++) {
                System.out.println(dw.getIdm().getName(c, m) + "=" + probs[m]);
                // TODO: remove
                // We don't bound the probabilities
                //                Assert.assertTrue(dw.getBounds().getLb(c,m) <= logProbs[c][m]);
                //                Assert.assertTrue(dw.getBounds().getUb(c,m) >= logProbs[c][m]);
            }
            System.out.println("");
        }
        return relaxSol;
    }

    private void adjustBounds(DmvDantzigWolfeRelaxation dw, double newL, double newU, boolean forward) {
        // Adjust bounds
        for (int c=0; c<dw.getIdm().getNumConds(); c++) {
            for (int m=0; m<dw.getIdm().getNumParams(c); m++) {
                DmvBounds origBounds = dw.getBounds();
                double lb = origBounds.getLb(c, m);
                double ub = origBounds.getUb(c, m);

                double deltU = newU - ub;
                double deltL = newL - lb;
                //double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
                DmvBoundsDelta deltas1 = new DmvBoundsDelta(c, m, Lu.UPPER, deltU);
                DmvBoundsDelta deltas2 = new DmvBoundsDelta(c, m, Lu.LOWER, deltL);
                if (forward) {
                    dw.forwardApply(deltas1);
                    dw.forwardApply(deltas2);
                } else {
                    dw.reverseApply(deltas1);
                    dw.reverseApply(deltas2);
                }
                System.out.println("l, u = " + dw.getBounds().getLb(c,m) + ", " + dw.getBounds().getUb(c,m));
            }
        }
    }
    
    @Test 
    public void testFracParseSum() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V P");
        sentences.addSentenceFromString("N V N N N");
        sentences.addSentenceFromString("N V P N");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

        for (int s = 0; s < sentences.size(); s++) {
            double sum = Vectors.sum(relaxSol.getFracChildren()[s]) + Vectors.sum(relaxSol.getFracRoots()[s]);
            System.out.println(s + " fracParseSum: " + sum);
            assertEquals(sum, sentences.get(s).size(), 1e-13);
        }
    }
    
    @Test
    public void testAdditionalCuttingPlanes() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("D N");
        sentences.addSentenceFromString("A N");
        sentences.addSentenceFromString("N V");
        sentences.addSentenceFromString("N V N N");
        sentences.addSentenceFromString("D N");

        int maxCuts = 5;
        double[] maxSums = new double[maxCuts];
        double prevSum = Double.POSITIVE_INFINITY;
        for (int numCuts=1; numCuts<maxCuts; numCuts++) {
            Prng.seed(12345);
            DmvDantzigWolfeRelaxation dw = getDw(sentences, numCuts);
            RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
            assertEquals(0.0, relaxSol.getScore(), 1e-13);
            double maxSum = 0.0;
            double[][] logProbs = relaxSol.getLogProbs();
            for (int c=0; c<logProbs.length; c++) {
                Vectors.exp(logProbs[c]);
                double sum = Vectors.sum(logProbs[c]);
                if (sum > maxSum) {
                    maxSum = sum;
                }
            }
            maxSums[numCuts] = maxSum;
            System.out.println("maxSums=" + Arrays.toString(maxSums));
            Assert.assertTrue(maxSum <= prevSum);
            prevSum = maxSum;
        }
        System.out.println("maxSums=" + Arrays.toString(maxSums));
   }

    private DmvDantzigWolfeRelaxation getDw(SentenceCollection sentences) {
        return getDw(sentences, 1);
    }
    
    /**
     * Helper function 
     * @return DW relaxation with 1 round of cuts, and 1 initial cut per parameter
     */
    public static DmvDantzigWolfeRelaxation getDw(SentenceCollection sentences, final int numCuts) {
        DmvSolution initSol = getInitFeasSol(sentences);
        System.out.println(initSol);
        CutCountComputer ccc = new CutCountComputer(){ 
            @Override
            public int getNumCuts(int numParams) {
                return numCuts;
            }
        };
        DmvDantzigWolfeRelaxation dw = new DmvDantzigWolfeRelaxation(sentences, new File("."), numCuts, ccc);
        dw.init(initSol.getTreebank());
        return dw;
    }
    
    public static DmvSolution getInitFeasSol(SentenceCollection sentences) {
        // Run Viterbi EM to get a reasonable starting incumbent solution
        int iterations = 25;        
        double lambda = 0.1;
        double convergenceRatio = 0.99999;
        int numRestarts = 10;

        ViterbiParser parser = new DmvCkyParser();
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio, numRestarts);
        // TODO: use random restarts
        trainer.train(sentences);
        
        DepTreebank treebank = trainer.getCounts();
        IndexedDmvModel idm = new IndexedDmvModel(sentences);
        DepProbMatrix dpm = DmvModelConverter.getDepProbMatrix((DmvModel)trainer.getModel(), sentences.getLabelAlphabet());
        double[][] logProbs = idm.getCmLogProbs(dpm);
        
        // We let the DmvProblemNode compute the score
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, trainer.getLogLikelihood());
        return sol;
    }
        
}