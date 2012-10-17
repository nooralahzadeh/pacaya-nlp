package edu.jhu.hltcoe.gridsearch.cpt;

import java.util.List;

import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;

public class BasicCptBoundsDeltaFactory implements CptBoundsDeltaFactory {

    private VariableSelector varSelector;
    private VariableSplitter varSplitter;
    
    public BasicCptBoundsDeltaFactory(VariableSelector varSelector, VariableSplitter varSplitter) {
        this.varSelector = varSelector;
        this.varSplitter = varSplitter;
    }

    @Override
    public List<CptBoundsDeltaList> getDmvBounds(DmvProblemNode node) {
        VariableId varId = varSelector.select(node);
        return varSplitter.split(node.getBounds(), varId);
    }

}
