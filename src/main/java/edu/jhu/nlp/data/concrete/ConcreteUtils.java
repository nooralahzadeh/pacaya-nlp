package edu.jhu.nlp.data.concrete;

import java.util.List;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.DependencyParse;
import edu.jhu.hlt.concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.Parse;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.SituationMentionSet;
import edu.jhu.hlt.concrete.TokenTagging;
import edu.jhu.hlt.concrete.Tokenization;

public class ConcreteUtils {

    private ConcreteUtils() { }

    public static TokenTagging getFirstXTags(Tokenization tokenization, String taggingType) {
        return getFirstXTagsWithName(tokenization, taggingType, null);
    }    

    public static TokenTagging getFirstXTagsWithName(Tokenization tokenization, String taggingType, String toolName) {
        if (!tokenization.isSetTokenTaggingList()) {
            return null;
        }
        List<TokenTagging> tokenTaggingLists = tokenization.getTokenTaggingList();
        for (int i = 0; i < tokenTaggingLists.size(); i++) {
            TokenTagging tt = tokenTaggingLists.get(i);
            if (tt.isSetTaggingType() && tt.getTaggingType().equals(taggingType)
                    && (toolName == null || tt.getMetadata().getTool().contains(toolName))) {
                return tt;
            }
        }
        return null;
    }
    
    public static DependencyParse getFirstDependencyParse(Tokenization tokenization) {
        return getFirstDependencyParseWithName(tokenization, null);
    }
    
    public static DependencyParse getFirstDependencyParseWithName(Tokenization tokenization, String toolName) {
        List<DependencyParse> parseList = tokenization.getDependencyParseList();
        if (parseList == null) {
            return null;
        }
        for (int i = 0; i < parseList.size(); i++) {
            DependencyParse dp = parseList.get(i);
            if (toolName == null || dp.getMetadata().getTool().contains(toolName)) {
                return dp;
            }
        }
        return null;
    }
    
    public static Parse getFirstParse(Tokenization tokenization) {
        return getFirstParseWithName(tokenization, null);
    }
    
    public static Parse getFirstParseWithName(Tokenization tokenization, String toolName) {
        List<Parse> parseList = tokenization.getParseList();
        if (parseList == null) {
            return null;
        }
        for (int i = 0; i < parseList.size(); i++) {
            Parse p = parseList.get(i);
            if (toolName == null || p.getMetadata().getTool().contains(toolName)) {
                return p;
            }
        }
        return null;
    }

    public static int getNumSents(Communication comm) {
        int n = 0;
        for (Section section : comm.getSectionList()) {
            n += section.getSentenceListSize();
        }
        return n;
    }
    
    public static EntityMentionSet getFirstEntityMentionSetWithName(Communication comm, String toolName) {
        List<EntityMentionSet> cEmsList = comm.getEntityMentionSetList();
        if (cEmsList == null) {
            return null;
        }
        for (EntityMentionSet cEms : cEmsList) {            
            if (toolName == null || cEms.getMetadata().getTool().contains(toolName)) {
                return cEms;
            }
        }
        return null;
    }
    
    public static SituationMentionSet getFirstSituationMentionSetWithName(Communication comm, String toolName) {
        List<SituationMentionSet> cSmsList = comm.getSituationMentionSetList();
        if (cSmsList == null) {
            return null;
        }
        for (SituationMentionSet cSms : cSmsList) {            
            if (toolName == null || cSms.getMetadata().getTool().contains(toolName)) {
                return cSms;
            }
        }
        return null;
    }
    
}
