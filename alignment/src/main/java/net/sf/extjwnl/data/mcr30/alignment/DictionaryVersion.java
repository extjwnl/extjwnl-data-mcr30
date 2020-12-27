package net.sf.extjwnl.data.mcr30.alignment;

import net.sf.extjwnl.dictionary.*;

import java.util.*;

import net.sf.extjwnl.dictionary.Dictionary;

/**
 * Descriptor for a dictionary version, based on {@link Dictionary.Version}.
 */
class DictionaryVersion
{
    final private String publisher;
    final private String language;
    final private String number;

    DictionaryVersion(final Dictionary dictionary)
    {
        this(
            dictionary.getVersion().getPublisher(),
            dictionary.getVersion().getLocale().getISO3Language(),
            String.format("%2.1f", dictionary.getVersion().getNumber()));
    }

    DictionaryVersion(
        final String publisher,
        final String language,
        final String number)
    {
        this.publisher = publisher;
        this.language = language;
        this.number = number;
    }

    /**
     * @see {@link Dictionary.Version#getPublisher}
     */
    String getPublisher()
    {
        return publisher;
    }

    /**
     * ISO 639-2 three-letter language code derived from {@link
     * Dictionary.Version#getLocale}.
     */
    String getLanguage()
    {
        return language;
    }
        
    /**
     * @see {@link Dictionary.Version#getNumber}
     */
    String getNumber()
    {
        return number;
    }

    @Override public String toString()
    {
        return String.format("%s-%s-%s", publisher, language, number);
    }

    @Override public boolean equals(Object other)
    {
        if (other == this) {
            return true;
        }
        if ((other == null) || (other.getClass() != this.getClass())) {
            return false;
        }
        return toString().equals(other.toString());
    }

    @Override public int hashCode()
    {
        return toString().hashCode();
    }
}
