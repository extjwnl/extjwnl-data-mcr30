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

    private static final Map<String, AlignmentTable> alignmentMap = new HashMap<>();

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
        final String wordnetSource,
        final String languageCode) throws JWNLException
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
     * @throws JWNLException if mapping or dictionary resources unavailable or
     * could not be accessed
     */
    public static Synset mapSynset(
        final Synset sourceSynset,
        final Dictionary targetDictionary)
        throws JWNLException
    {
        final SynsetMapper mapper = loadMapper(sourceSynset.getDictionary(), targetDictionary);
        return mapper.mapSynset(sourceSynset);
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
     * @return the loaded {@link SynsetMapper}
     *
     * @throws JWNLException if mapping or dictionary resources unavailable or
     * could not be accessed
     */
    public static SynsetMapper loadMapper(
        final Dictionary sourceDictionary,
        final Dictionary targetDictionary) throws JWNLException
    {
        final DictionaryVersion sourceVersion =
            new DictionaryVersion(sourceDictionary);
        final DictionaryVersion targetVersion =
            new DictionaryVersion(targetDictionary);

        // no-op case
        if (sourceVersion.equals(targetVersion)) {
            return new IdentitySynsetMapper();
        }

        final String alignmentKey = constructAlignmentKey(sourceVersion, targetVersion);

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
                    throw new JWNLException(new IllegalArgumentException(alignmentKey));
                }
            }
        }
        assert(table != null);
        return new AlignedSynsetMapper(table, targetDictionary);
    }

    private static String constructAlignmentKey(
        final DictionaryVersion sourceVersion,
        final DictionaryVersion targetVersion)
    {
        return String.format("%s => %s", sourceVersion, targetVersion);
    }

    private static String constructDataPath(
        final String dataParent,
        final String dataDir)
    {
        return String.format("net/sf/extjwnl/data/%s/%s", dataParent, dataDir);
    }

    private static Dictionary loadDictionary(
        final String dataParent,
        final String dataDir) throws JWNLException
    {
        final String propertiesPath = String.format(
            "/%s/res_properties.xml", constructDataPath(dataParent, dataDir));
        return Dictionary.getResourceInstance(propertiesPath);
    }

    private static void loadAlignmentTables(
        final DictionaryVersion firstVersion,
        final DictionaryVersion secondVersion) throws IOException
    {
        final String firstLanguage = firstVersion.getLanguage();
        final String secondLanguage = secondVersion.getLanguage();

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
            // English as intermediary; only makes sense once we add other
            // non-English languages beyond Spanish.
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
        final String englishNumber = englishVersion.getNumber();
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
        final AlignmentTable wn31to30 = new MapAlignmentTable();
        final AlignmentTable wn30to31 = new MapAlignmentTable();
        wn31to30.linkReverse(wn30to31);

        final String dataPath = constructDataPath("mcr30", "alignment");
        final String filePath = String.format("%s/wn31-30.csv", dataPath);
        // resource is bundled with this package, so should always
        // be present
        try(final InputStream stream = InterLingualIndex.class.
            getClassLoader().getResourceAsStream(filePath))
        {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            while (reader.ready()) {
                final String line = reader.readLine();
                final String [] fields = line.split(",");
                assert(fields.length == 2);
                final String synset31 = fields[0];
                final String synset30 = fields[1];
                assert(synset31.length() > 1);
                assert(synset30.length() > 1);
                final char pos31 = synset31.charAt(0);
                final char pos30 = synset30.charAt(0);
                if (pos30 != pos31) {
                    // ignore some infelicities in the mapping
                    continue;
                }
                final POS pos = POS.getPOSForKey(pos31);
                final long offset31 = Long.parseLong(synset31.substring(1));
                final long offset30 = Long.parseLong(synset30.substring(1));
                wn31to30.addMapping(pos, offset31, offset30);
            }
        }

        alignmentMap.put(constructAlignmentKey(PRINCETON30, PRINCETON31), wn30to31);
        alignmentMap.put(constructAlignmentKey(PRINCETON31, PRINCETON30), wn31to30);
    }

    private static void loadCompositionTable(
        final DictionaryVersion englishVersion,
        final DictionaryVersion nonEnglishVersion) throws IOException
    {
        final DictionaryVersion english30 = new DictionaryVersion(
            englishVersion.getPublisher(),
            englishVersion.getLanguage(),
            "3.0");
        loadAlignmentTables(
            nonEnglishVersion,
            english30);
        final AlignmentTable nonEngToEng30 = alignmentMap.get(
            constructAlignmentKey(nonEnglishVersion, english30));
        final String alignmentKey = constructAlignmentKey(PRINCETON30, PRINCETON31);
        AlignmentTable eng30To31 = alignmentMap.get(alignmentKey);
        if (eng30To31 == null) {
            loadWordnetTables();
            eng30To31 = alignmentMap.get(alignmentKey);
        }
        final AlignmentTable nonEngTo31 = new CompositionAlignmentTable(
            nonEngToEng30, eng30To31);
        final AlignmentTable eng31ToNon = new CompositionAlignmentTable(
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
        final DictionaryVersion englishVersion,
        final DictionaryVersion nonEnglishVersion) throws IOException
    {
        final String nonEnglishLanguage = nonEnglishVersion.getLanguage();
        final AlignmentTable engToNon = new MapAlignmentTable();
        final AlignmentTable nonToEng = new MapAlignmentTable();
        engToNon.linkReverse(nonToEng);

        final String dataPath = constructDataPath("mcr30", nonEnglishLanguage);
        final String iliPath = String.format("%s/ili.csv", dataPath);

        try(final InputStream stream = InterLingualIndex.class.
            getClassLoader().getResourceAsStream(iliPath))
        {
            if (stream == null) {
                // no translation table available for this language
                return;
            }
            final Map<POS, List<Long>> index = new HashMap<>();
            index.put(POS.NOUN, loadPOS(dataPath, "data.noun"));
            index.put(POS.VERB, loadPOS(dataPath, "data.verb"));
            index.put(POS.ADJECTIVE, loadPOS(dataPath, "data.adj"));
            index.put(POS.ADVERB, loadPOS(dataPath, "data.adv"));
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            while (reader.ready()) {
                final String line = reader.readLine();
                final String [] cols = line.split(",");
                assert(cols.length == 2);
                final String src = cols[0];
                final String target = cols[1];
                final char posChar = src.charAt(0);
                if (posChar == target.charAt(0)) {
                    assert(src.charAt(1) == '#');
                    final int srcIndex = Integer.parseInt(src.substring(2));
                    final long targetOffset = Long.parseLong(target.substring(1));
                    final POS pos = POS.getPOSForKey(posChar);
                    nonToEng.addMapping(pos, index.get(pos).get(srcIndex), targetOffset);
                }
            }
        }
        alignmentMap.put(
            constructAlignmentKey(englishVersion, nonEnglishVersion),
            engToNon);
        alignmentMap.put(
            constructAlignmentKey(nonEnglishVersion, englishVersion),
            nonToEng);
    }

    private static List<Long> loadPOS(
        final String dataPath,
        final String resourceName) throws IOException
    {
        final List<Long> list = new ArrayList<>();
        final String resourcePath = String.format("%s/%s", dataPath, resourceName);

        long pos = 0;
        try(final InputStream stream = InterLingualIndex.class.
            getClassLoader().getResourceAsStream(resourcePath))
        {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            while (reader.ready()) {
                final String line = reader.readLine();
                final long linePos = pos;
                pos += (line.getBytes("UTF-8").length + 1);
                if (!line.startsWith("#")) {
                    final long offset = Long.valueOf(line.split(" ")[0]);
                    assert(offset == linePos);
                    list.add(offset);
                }
            }
        }
        return list;
    }
}
