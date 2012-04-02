package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel.CM;
import edu.jhu.hltcoe.math.Multinomials;

public class RandomDmvBoundsDeltaFactory implements DmvBoundsDeltaFactory {

    private double[] freqs;
    private CM[] cms;
    
    public RandomDmvBoundsDeltaFactory(SentenceCollection sentences, IndexedDmvModel idm) {
        int[][] maxFreqCm = idm.getTotalMaxFreqCm();

        // Restructure the max freqs for efficient sampling
        int numCm = 0;
        for (int c=0; c<maxFreqCm.length; c++) {
            numCm += maxFreqCm[c].length;
        }
        cms = new CM[numCm];
        freqs = new double[numCm];
        
        for (int c=0, i=0; c<maxFreqCm.length; c++) {
            for (int m=0; m<maxFreqCm[c].length; m++) {
                cms[i] = new CM(c,m);
                freqs[i] = maxFreqCm[c][m]; 
                i++;
            }
        }

        // TODO: remove
        // We don't need this because we're zeroing out unused parameters
//        // Smooth this distribution by adding on the maximum possible frequency
//        double maxFreq = 0;
//        for (int i=0; i<freqs.length; i++) {
//            if (freqs[i] > maxFreq) {
//                maxFreq = freqs[i];
//            }
//        }
//        for (int i=0; i<freqs.length; i++) {
//            freqs[i] += maxFreq;
//        }
    }

    @Override
    public List<DmvBoundsDelta> getDmvBounds(DmvProblemNode dmvProblemNode) {
        DmvBounds origBounds = dmvProblemNode.getBounds();

        // Choose a model parameter with probability proportional to its 
        // possible occurence in the corpus
        // TODO: it might be better to consider the current frequency of use in
        // the parent node's feasible solution
        // TODO: this could be more efficient if we kept cumulative frequencies
        // and did binary search for the sampling value.
        int cmId = Multinomials.sampleFromProportions(freqs);
        int c = cms[cmId].get1();
        int m = cms[cmId].get2();


        return RegretDmvBoundsDeltaFactory.splitHalfProbSpace(origBounds, c, m);
    }

}