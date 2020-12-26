package net.sf.extjwnl.data.mcr30.alignment;

import net.sf.extjwnl.*;
import net.sf.extjwnl.dictionary.*;
import net.sf.extjwnl.data.*;

/**
 * Mapper for repeated used in translating word senses from one dictionary to another.
 */
public interface SynsetMapper
{
    /**
     * Maps a synset from one dictionary to another.
     *
     * @param sourceSynset the synset to be mapped from the source dictionary
     *
     * @return the synset corresponding to <code>sourceSynset</code> in the target
     * dictionary, or null if no mapping is available
     *
     * @throws JWNLException if dictionary unknown or resource unavailable
     */
    Synset mapSynset(Synset sourceSynset) throws JWNLException;
}

class SynsetIdentityMapper implements SynsetMapper
{
    @Override public Synset mapSynset(Synset sourceSynset)
    {
        return sourceSynset;
    }
}

class SynsetAlignedMapper implements SynsetMapper
{
    private AlignmentTable alignmentTable;

    private Dictionary targetDictionary;

    SynsetAlignedMapper(AlignmentTable alignmentTable, Dictionary targetDictionary)
    {
        this.alignmentTable = alignmentTable;
        this.targetDictionary = targetDictionary;
    }

    @Override public Synset mapSynset(Synset sourceSynset) throws JWNLException
    {
        POS pos = sourceSynset.getPOS();
        Long targetOffset = alignmentTable.lookup(pos, sourceSynset.getOffset());
        if (targetOffset == null) {
            return null;
        } else {
            return targetDictionary.getSynsetAt(pos, targetOffset);
        }
    }
}
