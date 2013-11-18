package edu.jhu.gm.maxent;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureTemplate;
import edu.jhu.gm.feat.FeatureTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Alphabet;

/**
 * A factor for FgExamples constructed from LogLinearExDesc objects.
 * @author mgormley
 */
public class LogLinearEDs {

    /**
     * A description of a collection of identical log-linear model examples.
     * 
     * @author mgormley
     * @author mmitchell
     */
    public static class LogLinearExDesc {
        private int count;
        private FeatureVector features;
        public LogLinearExDesc(int count, FeatureVector features) {
            this.count = count;
            this.features = features;
        }
        public int getCount() {
            return count;
        }
        public FeatureVector getFeatures() {
            return features;
        }
    }
    
    private static final Object TEMPLATE_KEY = "loglin";
    private final Alphabet<Feature> alphabet = new Alphabet<Feature>();
    private ArrayList<LogLinearExDesc> descList = new ArrayList<LogLinearExDesc>();

    public void addEx(int count, String... featNames) {
        FeatureVector features = new FeatureVector();
        for (String featName : featNames) {
            features.put(alphabet.lookupIndex(new Feature(featName)), 1.0);
        }
        LogLinearExDesc ex = new LogLinearExDesc(count, features);
        descList.add(ex);
    }
    
    public List<String> getStateNames() {
        List<String> names = new ArrayList<String>();
        for (LogLinearExDesc desc : descList) {
            StringBuilder sb = new StringBuilder();
            for (IntDoubleEntry entry : desc.getFeatures()) {
                sb.append(entry.index());
                sb.append(":");
            }
            names.add(sb.toString());
        }
        return names;
    }
    
    public FgExampleList getData() {
        FeatureTemplateList fts = new FeatureTemplateList();
        Var v0 = new Var(VarType.PREDICTED, descList.size(), "v0", getStateNames());
        fts.add(new FeatureTemplate(new VarSet(v0), alphabet, TEMPLATE_KEY));
        
        FgExampleMemoryStore data = new FgExampleMemoryStore(fts);
        int state=0;
        for (final LogLinearExDesc desc : descList) {
            for (int i=0; i<desc.getCount(); i++) {
                final VarConfig trainConfig = new VarConfig();
                trainConfig.put(v0, state);
                
                FactorGraph fg = new FactorGraph();
                v0 = new Var(VarType.PREDICTED, descList.size(), "v0", getStateNames());
                ExpFamFactor f0 = new ExpFamFactor(new VarSet(v0), TEMPLATE_KEY);
                fg.addFactor(f0);
                ObsFeatureExtractor featExtractor = new ObsFeatureExtractor() {
                    @Override
                    public FeatureVector calcObsFeatureVector(int factorId) {
                        // TODO: This doesn't do the right thing...we
                        // actually want features of the predicted state,
                        // which isn't possible to set when only looking at
                        // the observations.
                        // Instead we need to be aware of the VarConfig of the predicted vars.
                        return desc.getFeatures();
                    }
                    public void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred,
                            VarConfig goldConfig, FeatureTemplateList fts) {             
                        // Do nothing.               
                    }
                    public void clear() {
                        // Do nothing.
                    }
                };
                data.add(new FgExample(fg, trainConfig, featExtractor, fts));
            }
            state++;
        }
        return data;
    }

    public Alphabet<Feature> getAlphabet() {
        return alphabet;
    }
        
}