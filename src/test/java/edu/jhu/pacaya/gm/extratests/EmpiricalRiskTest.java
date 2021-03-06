package edu.jhu.pacaya.gm.extratests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReader;
import edu.jhu.nlp.data.simple.AnnoSentenceReader.AnnoSentenceReaderPrm;
import edu.jhu.nlp.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.pacaya.autodiff.ModuleTestUtils;
import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.data.FgExampleListBuilder.CacheType;
import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.train.AvgBatchObjective;
import edu.jhu.pacaya.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.pacaya.gm.train.DlFactory;
import edu.jhu.pacaya.gm.train.EmpiricalRisk.EmpiricalRiskFactory;
import edu.jhu.pacaya.gm.train.ExpectedRecall.ExpectedRecallFactory;
import edu.jhu.pacaya.gm.train.L2Distance.L2DistanceFactory;
import edu.jhu.pacaya.gm.train.ModuleObjective;
import edu.jhu.pacaya.gm.train.MtFactory;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.random.Prng;

public class EmpiricalRiskTest {
    
    @Before
    public void setUp() {
        Prng.seed(1l);
    }
    
    @Test
    public void testDpData() throws IOException {
        helpDpDataErma(new ExpectedRecallFactory(), RealAlgebra.getInstance());
        helpDpDataErma(new L2DistanceFactory(), RealAlgebra.getInstance());
        helpDpDataErma(new ExpectedRecallFactory(), LogSignAlgebra.getInstance());
        helpDpDataErma(new L2DistanceFactory(), LogSignAlgebra.getInstance());
    }

    private void helpDpDataErma(DlFactory dl, Algebra s) throws IOException {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);

        FgExampleList data = getDpData(ofc, 10);
        
        System.out.println("Num features: " + ofc.getNumParams());
        FgModel model = new FgModel(ofc.getNumParams());
        model.zero();
        
        MtFactory mtFactory = new EmpiricalRiskFactory(getErmaBpPrm(s), dl);
        ExampleObjective exObj = new ModuleObjective(data, mtFactory);
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model);

        System.out.println(DoubleArrays.toString(obj.getGradient(model.getParams()).toNativeArray(), "%.4g"));
                
        model.setRandomStandardNormal();        
        ModuleTestUtils.assertGradientCorrectByFd(obj, model.getParams(), 1e-5, 1e-7);
    }

    public static BeliefPropagationPrm getErmaBpPrm(Algebra s) {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.s = s;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        bpPrm.maxIterations = 1;        
        return bpPrm;
    }    
    
    public static FgExampleList getDpData(ObsFeatureConjoiner ofc, int featureHashMod) throws IOException {
        AnnoSentenceReaderPrm rPrm = new AnnoSentenceReaderPrm();
        rPrm.maxNumSentences = 3;
        rPrm.maxSentenceLength = 7;
        rPrm.useCoNLLXPhead = true;
        AnnoSentenceReader r = new AnnoSentenceReader(rPrm);
        r.loadSents(EmpiricalRiskTest.class.getResourceAsStream(LogLikelihoodFactoryTest.conllXExample), DatasetType.CONLL_X);        
        
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        AnnoSentenceCollection sents = r.getData();
        assertEquals(rPrm.maxNumSentences, sents.size());
        cs.init(sents);
        
        JointNlpFgExampleBuilderPrm prm = new JointNlpFgExampleBuilderPrm();
        prm.fgPrm.includeSrl = false;
        prm.fgPrm.dpPrm.linkVarType = VarType.PREDICTED;
        prm.fgPrm.dpPrm.useProjDepTreeFactor = true;
        prm.exPrm.cacheType = CacheType.NONE;
        prm.fgPrm.dpPrm.dpFePrm.featureHashMod = featureHashMod;
        
        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(prm, ofc, cs);
        FgExampleList data = builder.getData(sents);
        return data;
    }
    
}
