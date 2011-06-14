package edu.jhu.hltcoe.parse;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.model.Model;

public interface ViterbiSentenceParser {

    DepTree getViterbiParse(Sentence sentence, Model model);

}
