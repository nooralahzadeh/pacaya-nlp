package edu.jhu.hltcoe.parse;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.DmvModelFactory.RandomWeightGenerator;
import edu.jhu.hltcoe.util.Files;
import edu.jhu.hltcoe.util.Prng;


public class InitializedIlpViterbiParserWithDeltasTest {

    static {
        BasicConfigurator.configure();
    }
    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.setSeed(1234567890);
    }
    
    @Test
    public void testIdentityDeltaGen() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("cat ate mouse"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -40.56204981;

        DeltaGenerator deltaGen;

        DepTreebank treesStandard = IlpViterbiParserTest.getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, expectedParseWeight);

        deltaGen = new IdentityDeltaGenerator();
        DepTreebank treesDelta = getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight, expectedParseWeight);

        for (int i=0; i<treesStandard.size(); i++) {
            assertArrayEquals(treesStandard.get(i).getParents(), treesDelta.get(i).getParents());
        }
    }
    
    @Test
    public void testProjAndNonprojDeltaParsers() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("cat ate mouse"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        
        double expectedInitParseWeight;
        double expectedParseWeight;

        DeltaGenerator deltaGen;

        expectedInitParseWeight = -40.56204981;
        expectedParseWeight = -35.35388011;
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight, expectedInitParseWeight);

        expectedInitParseWeight = -41.12979411;
        expectedParseWeight = -39.12828011;
        deltaGen = new FactorDeltaGenerator(1.1, 2);
        getParses(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen, expectedParseWeight, expectedInitParseWeight);
    }
        
    public static DepTreebank getParses(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen, double expectedParseWeight, double expectedInitParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.CPLEX, 2, 128);
        InitializedIlpViterbiParserWithDeltas parser = new InitializedIlpViterbiParserWithDeltas(formulation, factory, deltaGen);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-13);
        
        File initCplexLog = new File(new File(parser.getInitWorkspace(), "ilp_parse_000"), "cplex.log");
        Matcher initMatch = Files.getFirstMatch(initCplexLog, Pattern.compile("Solution value = (.+)"));
        Assert.assertEquals(Double.parseDouble(initMatch.group(1)), expectedInitParseWeight, 1E-13);
        
        File cplexLog = new File(new File(parser.getWorkspace(), "ilp_parse_000"), "cplex.log");
        Matcher match = Files.getFirstMatch(cplexLog, Pattern.compile("defined initial solution with objective (.+)\\."));
        Assert.assertEquals(Double.parseDouble(match.group(1)), expectedInitParseWeight, 1E-4);
        
        return trees;
    }
        
}