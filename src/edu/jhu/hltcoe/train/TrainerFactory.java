package edu.jhu.hltcoe.train;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.parse.DeltaGenerator;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.FactorDeltaGenerator;
import edu.jhu.hltcoe.parse.FixedIntervalDeltaGenerator;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParser;
import edu.jhu.hltcoe.parse.IlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.parse.IlpViterbiSentenceParser;
import edu.jhu.hltcoe.parse.InitializedIlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.decomp.DeltaParseBlockFileWriter;
import edu.jhu.hltcoe.util.Command;

public class TrainerFactory {

    private static ViterbiParser evalParser = null;
    
    public static void addOptions(Options options) {
        options.addOption("a", "algorithm", true, "Inference algorithm.");
        options.addOption("i", "iterations", true, "Number of iterations.");
        options.addOption("i", "convergenceRatio", true, "Convergence ratio.");
        options.addOption("m", "model", true, "Model.");
        options.addOption("p", "parser", true, "Parser.");
        options.addOption("d", "deltaGenerator", true, "Delta generator.");
        options.addOption("in", "interval", true, "Only for fixed-interval delta generator.");
        options.addOption("fa", "factor", true, "Only for factor delta generator.");
        options.addOption("nps", "numPerSide", true, "For symmetric delta generators.");
        options.addOption("f", "formulation", true, "ILP formulation for parsing");
        options.addOption("l", "lambda", true, "Value for add-lambda smoothing.");
        options.addOption("t", "threads", true, "Number of threads for parallel impl");
        options.addOption("ilp", "ilpSolver", true, "The ILP solver to use " + IlpSolverId.getIdList());
        options.addOption("ilpwmm", "ilpWorkMemMegs", true, "The working memory allotted for the ILP solver in megabytes");
    }

    public static Trainer getTrainer(CommandLine cmd) throws ParseException {

        final String algorithm = Command.getOptionValue(cmd, "algorithm", "viterbi");
        final int iterations = Command.getOptionValue(cmd, "iterations", 10);
        final double convergenceRatio = Command.getOptionValue(cmd, "convergenceRatio", 0.99999);
        final String modelName = Command.getOptionValue(cmd, "model", "dmv");
        final String parserName = Command.getOptionValue(cmd, "parser", "ilp-sentence");
        final String deltaGenerator = Command.getOptionValue(cmd, "deltaGenerator", "fixed");
        final double interval = Command.getOptionValue(cmd, "interval", 0.01);
        final double factor = Command.getOptionValue(cmd, "factor", 1.1);
        final int numPerSide = Command.getOptionValue(cmd, "numPerSide", 2);
        final IlpFormulation formulation = getOptionValue(cmd, "formulation", IlpFormulation.DP_PROJ); 
        final double lambda = Command.getOptionValue(cmd, "lambda", 0.1);
        final int numThreads = Command.getOptionValue(cmd, "threads", 2);
        final String ilpSolver = Command.getOptionValue(cmd, "ilpSolver", "cplex");
        final double ilpWorkMemMegs = Command.getOptionValue(cmd, "ilpWorkMemMegs", 512.0);
        
        Trainer trainer = null;
        if (algorithm.equals("viterbi")) {
            ViterbiParser parser;
            if (modelName.equals("dmv")) {
                evalParser = new DmvCkyParser();
                
                IlpSolverFactory ilpSolverFactory = null;
                if (parserName.startsWith("ilp-")) {
                    IlpSolverId ilpSolverId = IlpSolverId.getById(ilpSolver);
                    ilpSolverFactory = new IlpSolverFactory(ilpSolverId, numThreads, ilpWorkMemMegs);
                    // TODO: make this an option
                    ilpSolverFactory.setBlockFileWriter(new DeltaParseBlockFileWriter(formulation));
                    
                }

                if (parserName.equals("cky")) {
                    parser = new DmvCkyParser();
                }else if (parserName.equals("ilp-sentence")) {
                    parser = new IlpViterbiSentenceParser(formulation, ilpSolverFactory);
                } else if (parserName.equals("ilp-corpus")) {
                    parser = new IlpViterbiParser(formulation, ilpSolverFactory);
                } else if (parserName.equals("ilp-deltas") || parserName.equals("ilp-deltas-init")) {
                    DeltaGenerator deltaGen;
                    if (deltaGenerator.equals("fixed-interval")) {
                        deltaGen = new FixedIntervalDeltaGenerator(interval, numPerSide);
                    } else if (deltaGenerator.equals("factor")) {
                        deltaGen = new FactorDeltaGenerator(factor, numPerSide);
                    } else {
                        throw new ParseException("Delta generator not supported: " + deltaGenerator);
                    }
                    if (parserName.equals("ilp-deltas")) {
                        parser = new IlpViterbiParserWithDeltas(formulation, ilpSolverFactory, deltaGen);
                    } else if (parserName.equals("ilp-deltas-init")) {
                        parser = new InitializedIlpViterbiParserWithDeltas(formulation, ilpSolverFactory, deltaGen, ilpSolverFactory);
                    } else {
                        throw new ParseException("Parser not supported: " + parserName);
                    }
                } else {
                    throw new ParseException("Parser not supported: " + parserName);
                }
            } else {
                throw new ParseException("Model not supported: " + modelName);
            }
            
            MStep<DepTreebank> mStep;
            ModelFactory modelFactory;
            if (modelName.equals("dmv")) {
                mStep = new DmvMStep(lambda);
                modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
            } else {
                throw new ParseException("Model not supported: " + modelName);
            }

            trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio);
        } else {
            throw new ParseException("Algorithm not supported: " + algorithm);
        }

        return trainer;
    }

    public static IlpFormulation getOptionValue(CommandLine cmd, String name, IlpFormulation defaultValue) {
        return cmd.hasOption(name) ? IlpFormulation.getById(cmd.getOptionValue(name)) : defaultValue;
    }
    
    /** 
     * TODO: This is a bit hacky, but convenient.
     */
    public static ViterbiParser getEvalParser() {
        return evalParser;
    }

}
