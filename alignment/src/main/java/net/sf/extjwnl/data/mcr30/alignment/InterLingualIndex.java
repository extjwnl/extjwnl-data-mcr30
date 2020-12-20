package net.sf.extjwnl.data.mcr30.alignment;

import net.sf.extjwnl.*;
import net.sf.extjwnl.dictionary.*;

public class InterLingualIndex
{
    /**
     * Gets a dictionary for a language from a known prepackaged source.
     *
     * @param wordnetSource one of <ul>
     * <li><code>wn31</code>: Princeton WordNet 3.1</li>
     * <li><code>wn31/map</code>: Princeton WordNet 3.1 (via serialized form)</li>
     * <li><code>wn30</code>: Princeton WordNet 3.0</li>
     * <li><code>wn21</code>: Princeton WordNet 2.1</li>
     * <li><code>mcr30</code>: Multilingual Central Repository (MCR) 3.0</li>
     * </ul>
     *
     * @param languageCode ISO 639-3 three-letter language code, e.g. <code>eng</code> or
     * <code>spa</code>
     *
     * @return loaded dictionary
     *
     * @throws JWNLException if dictionary unknown or resource unavailable
     */
    public static Dictionary getDictionary(
        String wordnetSource,
        String languageCode) throws JWNLException
    {
        if (languageCode.equals("eng")) {
            return loadDictionary("wordnet", wordnetSource);
        } else {
            return loadDictionary(wordnetSource, languageCode);
        }
    }

    private static Dictionary loadDictionary(String dataParent, String dataDir) throws JWNLException
    {
        String propertiesPath =
            "/net/sf/extjwnl/data/" + dataParent + "/" + dataDir + "/res_properties.xml";
        return Dictionary.getResourceInstance(propertiesPath);
    }
}
