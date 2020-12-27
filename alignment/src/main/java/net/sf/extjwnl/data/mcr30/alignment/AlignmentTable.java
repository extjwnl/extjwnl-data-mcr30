package net.sf.extjwnl.data.mcr30.alignment;

import java.util.*;

import net.sf.extjwnl.data.*;

/**
 * Abstraction for bidirectional synset mapping between dictionaries.
 */
abstract class AlignmentTable
{
    AlignmentTable reverse;

    void linkReverse(final AlignmentTable table)
    {
        reverse = table;
        table.reverse = this;
    }

    AlignmentTable getReverse()
    {
        return reverse;
    }
        
    abstract Long lookup(final POS pos, final long first);

    abstract void addMapping(
        final POS pos, final long first, final long second, final boolean withReverse);

    void addMapping(final POS pos, final long first, final long second)
    {
        addMapping(pos, first, second, true);
    }
}

/**
 * AlignmentTable implementation based on an in-memory map.
 */
class MapAlignmentTable extends AlignmentTable
{
    final Map<POS, Map<Long, Long>> forward = newMap();

    private Map<POS, Map<Long, Long>> newMap()
    {
        final Map<POS, Map<Long, Long>> map = new HashMap<POS, Map<Long, Long>>();
        for (POS pos : POS.values()) {
            map.put(pos, new HashMap<Long, Long>());
        }
        return map;
    }

    @Override void addMapping(
final POS pos, final long first, final long second, final boolean withReverse)
    {
        forward.get(pos).put(first, second);
        if (withReverse) {
            getReverse().addMapping(pos, second, first, false);
        }
    }
        
    @Override Long lookup(final POS pos, final long first)
    {
        return forward.get(pos).get(first);
    }
}

/**
 * AlignmentTable implementation which functionally composes two underlying maps.
 */
class CompositionAlignmentTable extends AlignmentTable
{
    final AlignmentTable t1, t2;

    CompositionAlignmentTable(final AlignmentTable t1, final AlignmentTable t2)
    {
        this.t1 = t1;
        this.t2 = t2;
    }
        
    @Override void addMapping(POS pos, long first, long second, boolean withReverse)
    {
        throw new UnsupportedOperationException();
    }
        
    @Override Long lookup(POS pos, long first)
    {
        final Long offset = t1.lookup(pos, first);
        if (offset == null) {
            return offset;
        } else {
            return t2.lookup(pos, offset);
        }
    }
}
