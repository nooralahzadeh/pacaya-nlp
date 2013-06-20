package edu.jhu.hltcoe.parse.cky;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.hltcoe.data.Tag;

public class CnfGrammarBuilderTest {

    public static final String timeFliesGrammarResource = "/edu/jhu/hltcoe/parse/cky/time-flies.gr";

    @Test
    public void testLoadGrammarFromFile() throws IOException {

        CnfGrammarBuilder builder = new CnfGrammarBuilder();
        builder.loadFromResource(timeFliesGrammarResource);
        
        CnfGrammar grammar = builder.getGrammar();
        
        System.out.println(grammar);
        assertEquals(5, grammar.getNumLexicalTypes());
        assertEquals(8, grammar.getNumNonTerminals());
        
        // Only X -> NP VP rule is S -> NP VP
        assertEquals(
                1,
                grammar.getBinaryRulesWithChildren(
                        grammar.getNtAlphabet().lookupIndex(new Tag("NP")),
                        grammar.getNtAlphabet().lookupIndex(new Tag("VP"))).length);
    }

}