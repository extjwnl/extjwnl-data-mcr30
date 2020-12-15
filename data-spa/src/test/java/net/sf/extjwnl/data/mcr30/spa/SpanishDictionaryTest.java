package net.sf.extjwnl.data.mcr30.spa;

import org.junit.*;

import net.sf.extjwnl.*;
import net.sf.extjwnl.dictionary.*;
import net.sf.extjwnl.data.*;

public class SpanishDictionaryTest
{
    @Test
    public void loadDictionary() throws JWNLException
    {
        Dictionary dict = Dictionary.getDefaultResourceInstance();
        Assert.assertNotNull(dict);
        IndexWord mujer = dict.getIndexWord(POS.NOUN, "mujer");
        Assert.assertNotNull(mujer);
        IndexWord chupacabra = dict.getIndexWord(POS.NOUN, "chupacabra");
        Assert.assertNull(chupacabra);
    }
}
