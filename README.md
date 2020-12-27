# About

**extjwnl-data-mcr30** prepackages jars with wordnet data from the Multilingual
Central Repository 3.0 (2016 release; currently only the Spanish portion).

A configuration file is included to make it extremely easy to use
these resources in your project.

# Getting started

In your `pom.xml`:

```xml
<dependency>
    <groupId>net.sf.extjwnl</groupId>
    <artifactId>extjwnl</artifactId>
    <version>1.8.0</version>
</dependency>
<dependency>
    <groupId>net.sf.extjwnl.mcr</groupId>
    <artifactId>extjwnl-data-spa-mcr30</artifactId>
    <version>1.0.0</version>
</dependency>
```

In your code:

```java
import net.sf.extjwnl.dictionary.*;

Dictionary d = Dictionary.getDefaultResourceInstance();
```

# Mapping Between Dictionaries

**extjwnl-data-mcr30** also contains an **alignment** module which supports
loading multiple dictionaries and mapping word senses between them.  To
use it, you first need the following additional dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>net.sf.extjwnl.mcr</groupId>
    <artifactId>extjwnl-data-alignment-mcr30</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then you can load the MCR 3.0 Spanish wordnet together with two
versions (3.0 and 3.1) of Princeton WordNet:

```java
import net.sf.extjwnl.dictionary.*;
import net.sf.extjwnl.data.mcr30.alignment.*;

Dictionary spa = InterLingualIndex.getDictionary("mcr30", "spa");
Dictionary wn31 = InterLingualIndex.getDictionary("wn31", "eng");
Dictionary wn30 = InterLingualIndex.getDictionary("wn30", "eng");
```

After that, if you have a Spanish synset, you can find the corresponding
English synset (if a mapping exists):

```java
Synset englishSynset = InterLingualIndex.mapSynset(spanishSynset, wn31);
```

If you need to map lots of synsets, then use the `SynsetMapper` interface
instead:

```java
SynsetMapper mapper = InterLingualIndex.loadMapper(spa, wn31);
Synset englishSynset1 = mapper.mapSynset(spanishSynset1);
Synset englishSynset2 = mapper.mapSynset(spanishSynset2);
...
```

# Acknowledgements

The data for this package comes from the [Multilingual Central Repository (MCR)](https://adimen.si.ehu.es/web/MCR):

Aitor Gonzalez-Agirre, Egoitz Laparra and German Rigau (2012)
[Multilingual Central Repository version 3.0: upgrading a very large lexical knowledge base](http://adimen.si.ehu.es/~rigau/publications/gwc12-glr.pdf). In Proceedings of the 6th Global WordNet Conference (GWC 2012) Matsue, Japan.

    @InProceedings{Gonzalez-Agirre:Laparra:Rigau:2012,
      author = "Aitor Gonzalez-Agirre and Egoitz Laparra and German Rigau",
      title = "Multilingual Central Repository version 3.0: upgrading a very large lexical knowledge base",
      booktitle = "Proceedings of the 6th Global WordNet Conference (GWC 2012)",
      year = 2012,
      address = "Matsue",
    }

This package is designed for use with
[extjwnl](https://github.com/extjwnl/extjwnl).  The resource bundling
is based on the pattern set by
[extjwnl-data-wn31](https://github.com/extjwnl/extjwnl-data-wn31) for
the English-language Princeton WordNet 3.1.

Princeton University "About WordNet."
[WordNet](http://wordnet.princeton.edu). Princeton University. 2010.

MCR data is converted into extjwnl format via a modified version of the [wn-mcr-transform](https://github.com/pln-fing-udelar/wn-mcr-transform) script.  You can find the modified version [here](https://github.com/lingeringsocket/wn-mcr-transform).

The MCR is aligned with Princeton WordNet 3.0, so for realigning to Princeton WordNet 3.1, we use the 3.0->3.1 `mapping_wordnet.json` from:

    @misc{ZendelWordNetConv19,
      author = {Zendel, Oliver},
      title = {WordNet v3.0 vs. v3.1 mapping},
      year = {2019},
      publisher = {GitHub},
      journal = {GitHub repository},
      howpublished = {\url{https://github.com/ozendelait/wordnet-to-json}},
      commit = {7521b70937355e826ea7e028a615108cdb18d0ee}
    }

# Stemming

Language-specific stemming rules are packaged in each data module; for
example, [here](lang-spa/src/main/resources/net/sf/extjwnl/data/mcr30/spa/res_properties.xml)
are the Spanish-specific stemming rules.

# Exceptional Forms

For Spanish, exceptional forms (irregular verb conjugations, noun pluralizations, and adjective pluralizations) are enumerated using
the [morphala](https://github.com/lingeringsocket/morphala) project.  All lemmas from the MCR dictionary are run through morphala's conjugation/pluralization routines.  From the resulting derived form, we attempt to reverse-derive the lemma as a base form via the standard `DetachSuffixesOperation`.  When this fails, we treat the derived form as an exception and add it to `supplemental_spa.txt`.
