# About

extjwnl-data-mcr30 prepackages jars with wordnet data from the Multilingual
Central Repository 3.0 (2016 release; currently only the Spanish portion).

A configuration file is included to make it extremely easy to use
these resources in your project.

# Getting started

In the pom.xml:

```xml
<dependency>
    <groupId>net.sf.extjwnl</groupId>
    <artifactId>extjwnl</artifactId>
    <version>1.8.0</version>
</dependency>
<dependency>
    <groupId>net.sf.extjwnl.mcr</groupId>
    <artifactId>extjwnl-data-spa-mcr30</artifactId>
    <version>0.0.1</version>
</dependency>
```

In the code:

```java
Dictionary d = Dictionary.getDefaultResourceInstance();
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

# Inter-Language Index

The mapping table is provided in file `translation.csv`, a comma-separated file with three columns:

* 0-based line number in non-English synset file (e.g. `a#000030` means the 31st line of `data.adj` in extjwnl-data-mcr30 language-specific data module)
* synset offset in (English) Princeton WordNet 3.1 (e.g. `a6903` means the synset at character offset 6903 of `data.adj` in Wordnet 3.0)
* synset offset in (English) Princeton WordNet 3.0 (e.g. `a00006885` means the synset at character offset 6885 of `data.adj` in Wordnet 3.1)

The mapping table can be loaded in order to translate between non-English and English word senses.

# Stemming

Language-specific stemming rules are packaged in each data module; for
example, the Spanish-specific stemming rules are in
[extjwnl_data_spa_mcr30.xml](data-spa/src/main/resources/extjwnl_data_spa_mcr30.xml)

# Exceptional Forms

For Spanish, exceptional forms (irregular verb conjugations, noun pluralizations, and adjective pluralizations) are enumerated using
the [morphala](https://github.com/lingeringsocket/morphala) project.  All lemmas from the MCR dictionary are run through morphala's conjugation/pluralization routines.  From the resulting derived form, we attempt to reverse-derive the lemma as a base form via the standard `DetachSuffixesOperation`.  When this fails, we treat the derived form as an exception and add it to `supplemental_spa.txt`.
