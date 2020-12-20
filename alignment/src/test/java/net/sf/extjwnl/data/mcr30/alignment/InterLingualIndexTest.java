package net.sf.extjwnl.data.mcr30.alignment;

import org.junit.*;

import net.sf.extjwnl.*;
import net.sf.extjwnl.dictionary.*;
import net.sf.extjwnl.data.*;

public class InterLingualIndexTest
{
    @Test
    public void verifyDictionaries()
        throws JWNLException
    {
        Dictionary wn31 = InterLingualIndex.getDictionary("wn31", "eng");
        checkDictionary(wn31, "zumbooruk", "chupacabra");

        Dictionary wn31map = InterLingualIndex.getDictionary("wn31/map", "eng");
        checkDictionary(wn31map, "zumbooruk", "chupacabra");

        Dictionary wn30 = InterLingualIndex.getDictionary("wn30", "eng");
        checkDictionary(wn30, "zymology", "zumbooruk");

        Dictionary wn21 = InterLingualIndex.getDictionary("wn21", "eng");
        checkDictionary(wn21, "woman", "zymology");

        Dictionary spa = InterLingualIndex.getDictionary("mcr30", "spa");
        checkDictionary(spa, "mujer", "woman");
    }

    private void checkDictionary(Dictionary dict, String expectedNoun, String unexpectedNoun)
        throws JWNLException
    {
        Assert.assertNotNull(dict);
        Assert.assertNotNull(dict.getIndexWord(POS.NOUN, expectedNoun));
        Assert.assertNull(dict.getIndexWord(POS.NOUN, unexpectedNoun));
    }
}
