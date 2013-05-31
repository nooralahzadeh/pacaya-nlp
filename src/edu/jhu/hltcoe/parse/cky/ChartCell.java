package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.parse.cky.Chart.BackPointer;

/**
 * Chart cell for a chart parsing algorithm.
 * 
 * @author mgormley
 *
 */
public interface ChartCell {
    
    public BackPointer getBp(int symbol);
    
    public double getMaxScore(int symbol);
    
    public int[] getNts();

    public void updateCell(int mid, Rule r, double score);

    public MaxScoresSnapshot getMaxScoresSnapshot();

    public void close();


    /**
     * Ensures that the chart cell is open and all future calls will be just as
     * if it was newly constructed.
     */
    public void reset();

}