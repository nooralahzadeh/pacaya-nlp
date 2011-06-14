package edu.jhu.hltcoe.parse;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.DmvModelFactory.RandomWeightGenerator;


public class IlpViterbiParserWithDeltasTest {

    private final static double lambda = 0.1;

    @Test
    public void testProjParses() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("cat ate mouse"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        
        // flow projective parsing
        DeltaGenerator deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        DepTreebank npFlowTrees = getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen);
        //DepTreebank pFlowTrees = getParses(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen);
    }
    
    public DepTreebank getParses(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen) {
        IlpViterbiParserWithDeltas parser = new IlpViterbiParserWithDeltas(formulation, 2, deltaGen);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        return trees;
    }
    
}
