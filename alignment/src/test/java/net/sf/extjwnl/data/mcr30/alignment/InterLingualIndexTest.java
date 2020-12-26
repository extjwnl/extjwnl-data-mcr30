package net.sf.extjwnl.data.mcr30.alignment;

import org.junit.*;

import net.sf.extjwnl.*;
import net.sf.extjwnl.dictionary.*;
import net.sf.extjwnl.data.*;

/**
 * JUnit tests for {@link InterLingualIndex}.
 */
public class InterLingualIndexTest
{
    private static Dictionary wn31;
    private static Dictionary wn31map;
    private static Dictionary wn30;
    private static Dictionary wn21;
    private static Dictionary spa;

    @BeforeClass
    public static void setUpClass() throws JWNLException
    {
        wn31 = InterLingualIndex.getDictionary("wn31", "eng");
        wn31map = InterLingualIndex.getDictionary("wn31/map", "eng");
        wn30 = InterLingualIndex.getDictionary("wn30", "eng");
        wn21 = InterLingualIndex.getDictionary("wn21", "eng");
        spa = InterLingualIndex.getDictionary("mcr30", "spa");
    }

    @Test
    public void verifyDictionaries() throws JWNLException
    {
        // for each dictionary, verify one word that should exist,
        // and another word which should not
        checkDictionary(wn31, "zumbooruk", "chupacabra");
        checkDictionary(wn31map, "zumbooruk", "chupacabra");
        checkDictionary(wn30, "zymology", "zumbooruk");
        checkDictionary(wn21, "woman", "zymology");
        checkDictionary(spa, "mujer", "woman");
    }

    @Test
    public void verifyInterLanguage() throws JWNLException
    {
        // check word sense mapping between Spanish and English
        // for both supported Princeton WordNet versions
        checkClaustrophobia(wn30, 14383252);
        checkClaustrophobia(wn31, 14406657);
    }

    @Test
    public void verifyMapperReuse() throws JWNLException
    {
        // verify that we can load a mapper and then reuse it
        // for more than one synset
        Dictionary eng = wn30;
        SynsetMapper mapper = InterLingualIndex.loadMapper(eng, spa);

        Synset claustrophobia = eng.getSynsetAt(POS.NOUN, 14383252);
        Assert.assertTrue(claustrophobia.containsWord("claustrophobia"));

        Synset claustrofobia = mapper.mapSynset(claustrophobia);
        Assert.assertNotNull(claustrofobia);
        Assert.assertTrue(claustrofobia.containsWord("claustrofobia"));

        Synset acrophobia = eng.getSynsetAt(POS.NOUN, 14382766);
        Assert.assertTrue(acrophobia.containsWord("acrophobia"));

        Synset acrofobia = mapper.mapSynset(acrophobia);
        Assert.assertNotNull(acrofobia);
        Assert.assertTrue(acrofobia.containsWord("acrofobia"));
    }

    @Test
    public void verifyIntraLanguage() throws JWNLException
    {
        // verify that we can map within English across WordNet versions
        long offset31 = 7558676;
        long offset30 = 7543288;

        Synset love31 = wn31.getSynsetAt(POS.NOUN, offset31);
        Assert.assertTrue(love31.containsWord("love"));
        Assert.assertEquals(offset31, love31.getOffset());

        // map from 3.1 to 3.0
        Synset love30 = InterLingualIndex.mapSynset(love31, wn30);
        Assert.assertNotNull(love30);
        Assert.assertTrue(love30.containsWord("love"));
        Assert.assertEquals(offset30, love30.getOffset());

        // map from 3.0 back to 3.1
        Synset reverse = InterLingualIndex.mapSynset(love30, wn31);
        Assert.assertNotNull(reverse);
        Assert.assertEquals(love31, reverse);
    }

    private void checkDictionary(
        Dictionary dict,
        String expectedNoun,
        String unexpectedNoun) throws JWNLException
    {
        Assert.assertNotNull(dict);
        Assert.assertNotNull(dict.getIndexWord(POS.NOUN, expectedNoun));
        Assert.assertNull(dict.getIndexWord(POS.NOUN, unexpectedNoun));
    }

    private void checkClaustrophobia(
        Dictionary eng,
        long offset) throws JWNLException
    {
        Synset claustrophobia = eng.getSynsetAt(POS.NOUN, offset);
        Assert.assertTrue(claustrophobia.containsWord("claustrophobia"));

        // map from English to Spanish
        Synset claustrofobia = InterLingualIndex.mapSynset(claustrophobia, spa);
        Assert.assertNotNull(claustrofobia);
        Assert.assertTrue(claustrofobia.containsWord("claustrofobia"));

        // map from Spanish back to English
        Synset reverse = InterLingualIndex.mapSynset(claustrofobia, eng);
        Assert.assertNotNull(reverse);
        Assert.assertEquals(claustrophobia, reverse);
    }
}
