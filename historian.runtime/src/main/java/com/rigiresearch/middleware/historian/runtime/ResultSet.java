package com.rigiresearch.middleware.historian.runtime;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

/**
 * A result set.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 * @param <K> They type for the entry's key
 * @param <V> The type for the entry's value
 */
@SuppressWarnings("checkstyle:IllegalType")
public final class ResultSet<K, V> extends HashSet<Map.Entry<K, V>> {

    /**
     * Serial version UId.
     */
    private static final long serialVersionUID = 4030681789189584501L;

    /**
     * Whether this set will contain only one element.
     */
    @Getter
    private final boolean singleton;

    /**
     * Tertiary constructor.
     * @param key The key of the only element this map will contain
     * @param value The value of the only element this map will contain
     */
    public ResultSet(final K key, final V value) {
        this(
            Collections.singleton(
                new AbstractMap.SimpleEntry<>(key, value)
            ),
            true
        );
    }

    /**
     * Secondary constructor.
     * @param singleton Whether this set will contain only one element
     */
    public ResultSet(final boolean singleton) {
        this(Collections.emptySet(), singleton);
    }

    /**
     * Primary constructor.
     * @param entries The initial elements
     * @param singleton Whether this set will contain only one element
     */
    public ResultSet(final Set<Map.Entry<K, V>> entries,
        final boolean singleton) {
        super(entries);
        this.singleton = singleton;
    }

    /**
     * Adds a new entry composed of the given key and value.
     * @param key The element key
     * @param value The element value
     * @return True if this set did not already contain the specified
     *  element
     */
    public boolean addEntry(final K key, final V value) {
        return this.add(
            new AbstractMap.SimpleEntry<>(key, value)
        );
    }

}
