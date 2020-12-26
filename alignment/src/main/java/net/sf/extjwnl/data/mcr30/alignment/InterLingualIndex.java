package net.sf.extjwnl.data.mcr30.alignment;

import net.sf.extjwnl.*;
import net.sf.extjwnl.dictionary.*;
import net.sf.extjwnl.data.*;

import java.io.*;
import java.util.*;

import net.sf.extjwnl.dictionary.Dictionary;

/**
 * Singleton class for loading dictionaries for different languages
 * and mapping word senses between them.  This class and the mappers returned
 * by it are safe for access by multiple threads concurrently.
 *
 *<p>
 *
 * Note that this class loads resource files into memory on demand for mapping
 * purposes, and once loaded, they are never unloaded.
 */
public class InterLingualIndex
{
    private static final String PRINCETON = "Princeton";

    private static final String ENG639_3 = "eng";

    private static final DictionaryVersion PRINCETON30 =
        new DictionaryVersion(PRINCETON, ENG639_3, "3.0");

    private static final DictionaryVersion PRINCETON31 =
        new DictionaryVersion(PRINCETON, ENG639_3, "3.1");

    private static Map<String, AlignmentTable> alignmentMap = new HashMap<>();

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
        if (languageCode.equals(ENG639_3)) {
            return loadDictionary("wordnet", wordnetSource);
        } else {
            return loadDictionary(wordnetSource, languageCode);
        }
    }

    /**
     * Maps a synset from one dictionary to another, automatically using
     * the correct interlingual index (if one is available) for word sense
     * alignment.
     *
     * @param sourceSynset the synset to be mapped from the source dictionary
     *
     * @param targetDictionary the target dictionary into which
     * <code>sourceSynset</code> should be mapped
     *
     * @return the synset corresponding to <code>sourceSynset</code> in the target
     * dictionary, or null if no mapping is available
     *
     * @throws JWNLException if mapping or dictionary resources could not be accessed
     */
    public static Synset mapSynset(Synset sourceSynset, Dictionary targetDictionary)
        throws JWNLException
    {
        SynsetMapper mapper = loadMapper(sourceSynset.getDictionary(), targetDictionary);
        if (mapper == null) {
            return null;
        } else {
            return mapper.mapSynset(sourceSynset);
        }
    }

    /**
     * Loads a mapper from one dictionary to another, automatically using
     * the correct interlingual index (if one is available) for word sense
     * alignment.  The mapper can be used over and over to translate
     * different synsets.
     *
     * @param sourceDictionary the source dictionary from which synsets will be mapped
     *
     * @param targetDictionary the target dictionary into which
     * synsets will be mapped
     *
     * @return the loaded {@link SynsetMapper}, or null if no mapping is available
     *
     * @throws JWNLException if mapping or dictionary resources could not be accessed
     */
    public static SynsetMapper loadMapper(
        Dictionary sourceDictionary,
        Dictionary targetDictionary) throws JWNLException
    {
        final DictionaryVersion sourceVersion =
            new DictionaryVersion(sourceDictionary);
        final DictionaryVersion targetVersion =
            new DictionaryVersion(targetDictionary);

        // no-op case
        if (sourceVersion.equals(targetVersion)) {
            return new SynsetIdentityMapper();
        }

        String alignmentKey = constructAlignmentKey(sourceVersion, targetVersion);

        AlignmentTable table;
        synchronized(alignmentMap) {
            // table already loaded?
            table = alignmentMap.get(alignmentKey);
            if (table == null) {
                // nope, try to load it
                try {
                    loadAlignmentTables(sourceVersion, targetVersion);
                } catch (IOException ex) {
                    throw new JWNLIOException(ex);
                }
                table = alignmentMap.get(alignmentKey);
                if (table == null) {
                    // table not available, give up
                    return null;
                }
            }
        }
        assert(table != null);
        return new SynsetAlignedMapper(table, targetDictionary);
    }

    private static String constructAlignmentKey(
        DictionaryVersion sourceVersion,
        DictionaryVersion targetVersion)
    {
        return String.format("%s => %s", sourceVersion, targetVersion);
    }

    private static String constructDataPath(
        String dataParent,
        String dataDir)
    {
        return String.format("net/sf/extjwnl/data/%s/%s", dataParent, dataDir);
    }

    private static Dictionary loadDictionary(
        String dataParent,
        String dataDir) throws JWNLException
    {
        String propertiesPath = String.format(
            "/%s/res_properties.xml", constructDataPath(dataParent, dataDir));
        return Dictionary.getResourceInstance(propertiesPath);
    }

    private static void loadAlignmentTables(
        DictionaryVersion firstVersion,
        DictionaryVersion secondVersion) throws IOException
    {
        String firstLanguage = firstVersion.getLanguage();
        String secondLanguage = secondVersion.getLanguage();

        if (firstLanguage.equals(secondLanguage)) {
            if (firstLanguage.equals(ENG639_3) &&
                (!firstVersion.getNumber().equals(secondVersion.getNumber())))
            {
                loadWordnetTables();
            }
            return;
        }
        if (!firstLanguage.equals(ENG639_3) && !secondLanguage.equals(ENG639_3)) {
            // TODO: mappings directly between non-English languages using
            // English as intermediary
            return;
        }

        DictionaryVersion englishVersion;
        DictionaryVersion nonEnglishVersion;
        if (firstLanguage.equals(ENG639_3)) {
            englishVersion = firstVersion;
            nonEnglishVersion = secondVersion;
        } else {
            englishVersion = secondVersion;
            nonEnglishVersion = firstVersion;
        }

        // currently only Princeton WordNet 3.0 or 3.1 is supported for English
        String englishNumber = englishVersion.getNumber();
        if (!englishVersion.getPublisher().equals("Princeton")) {
            return;
        }
        // currently only MCR 3.0 is supported for non-English
        if (!nonEnglishVersion.getPublisher().equals("MCR")) {
            return;
        }
        if (!nonEnglishVersion.getNumber().equals("3.0")) {
            return;
        }

        if (englishNumber.equals("3.1")) {
            loadCompositionTable(englishVersion, nonEnglishVersion);
            return;
        }
        if (!englishNumber.equals("3.0")) {
            return;
        }
        loadMCR(englishVersion, nonEnglishVersion);
    }

    private static void loadWordnetTables()
        throws IOException
    {
        AlignmentTable wn31to30 = new MapAlignmentTable();
        AlignmentTable wn30to31 = new MapAlignmentTable();
        wn31to30.linkReverse(wn30to31);

        String dataPath = constructDataPath("mcr30", "alignment");
        String filePath = String.format("%s/wn31-30.csv", dataPath);
        // resource is bundled with this package, so should always
        // be present
        InputStream stream = InterLingualIndex.class.
            getClassLoader().getResourceAsStream(filePath);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                String [] fields = line.split(",");
                assert(fields.length == 2);
                String synset31 = fields[0];
                String synset30 = fields[1];
                assert(synset31.length() > 1);
                assert(synset30.length() > 1);
                char pos31 = synset31.charAt(0);
                char pos30 = synset30.charAt(0);
                if (pos30 != pos31) {
                    // ignore some infelicities in the mapping
                    continue;
                }
                POS pos = POS.getPOSForKey(pos31);
                long offset31 = Long.parseLong(synset31.substring(1));
                long offset30 = Long.parseLong(synset30.substring(1));
                wn31to30.addMapping(pos, offset31, offset30);
            }
        } finally {
            stream.close();
        }
        alignmentMap.put(constructAlignmentKey(PRINCETON30, PRINCETON31), wn30to31);
        alignmentMap.put(constructAlignmentKey(PRINCETON31, PRINCETON30), wn31to30);
    }

    private static void loadCompositionTable(
        DictionaryVersion englishVersion,
        DictionaryVersion nonEnglishVersion) throws IOException
    {
        DictionaryVersion english30 = new DictionaryVersion(
            englishVersion.getPublisher(),
            englishVersion.getLanguage(),
            "3.0");
        loadAlignmentTables(
            nonEnglishVersion,
            english30);
        AlignmentTable nonEngToEng30 = alignmentMap.get(
            constructAlignmentKey(nonEnglishVersion, english30));
        String alignmentKey = constructAlignmentKey(PRINCETON30, PRINCETON31);
        AlignmentTable eng30To31 = alignmentMap.get(alignmentKey);
        if (eng30To31 == null) {
            loadWordnetTables();
            eng30To31 = alignmentMap.get(alignmentKey);
        }
        AlignmentTable nonEngTo31 = new CompositionAlignmentTable(
            nonEngToEng30, eng30To31);
        AlignmentTable eng31ToNon = new CompositionAlignmentTable(
            eng30To31.getReverse(), nonEngToEng30.getReverse());
        nonEngTo31.linkReverse(eng31ToNon);
        alignmentMap.put(
            constructAlignmentKey(englishVersion, nonEnglishVersion),
            eng31ToNon);
        alignmentMap.put(
            constructAlignmentKey(nonEnglishVersion, englishVersion),
            nonEngTo31);
    }

    private static void loadMCR(
        DictionaryVersion englishVersion,
        DictionaryVersion nonEnglishVersion) throws IOException
    {
        String nonEnglishLanguage = nonEnglishVersion.getLanguage();
        AlignmentTable engToNon = new MapAlignmentTable();
        AlignmentTable nonToEng = new MapAlignmentTable();
        engToNon.linkReverse(nonToEng);

        String dataPath = constructDataPath("mcr30", nonEnglishLanguage);
        String iliPath = String.format("%s/ili.csv", dataPath);

        InputStream stream = InterLingualIndex.class.
            getClassLoader().getResourceAsStream(iliPath);
        if (stream == null) {
            // no translation table available for this language
            return;
        }
        try {
            Map<POS, List<Long>> index = new HashMap<>();
            index.put(POS.NOUN, loadPOS(dataPath, "data.noun"));
            index.put(POS.VERB, loadPOS(dataPath, "data.verb"));
            index.put(POS.ADJECTIVE, loadPOS(dataPath, "data.adj"));
            index.put(POS.ADVERB, loadPOS(dataPath, "data.adv"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                String [] cols = line.split(",");
                assert(cols.length == 2);
                String src = cols[0];
                String target = cols[1];
                char posChar = src.charAt(0);
                if (posChar == target.charAt(0)) {
                    assert(src.charAt(1) == '#');
                    int srcIndex = Integer.parseInt(src.substring(2));
                    long targetOffset = Long.parseLong(target.substring(1));
                    POS pos = POS.getPOSForKey(posChar);
                    nonToEng.addMapping(pos, index.get(pos).get(srcIndex), targetOffset);
                }
            }
        } finally {
            stream.close();
        }
        alignmentMap.put(
            constructAlignmentKey(englishVersion, nonEnglishVersion),
            engToNon);
        alignmentMap.put(
            constructAlignmentKey(nonEnglishVersion, englishVersion),
            nonToEng);
    }

    private static List<Long> loadPOS(
        String dataPath,
        String resourceName) throws IOException
    {
        List<Long> list = new ArrayList<>();
        String resourcePath = String.format("%s/%s", dataPath, resourceName);
        InputStream stream = InterLingualIndex.class.
            getClassLoader().getResourceAsStream(resourcePath);
        long pos = 0;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                long linePos = pos;
                pos += (line.getBytes("UTF-8").length + 1);
                if (!line.startsWith("#")) {
                    long offset = Long.valueOf(line.split(" ")[0]);
                    assert(offset == linePos);
                    list.add(offset);
                }
            }
        } finally {
            stream.close();
        }
        return list;
    }
}
