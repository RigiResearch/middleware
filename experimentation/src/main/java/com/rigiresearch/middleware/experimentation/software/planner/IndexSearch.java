package com.rigiresearch.middleware.experimentation.software.planner;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Value;

/**
 * Index search in a list.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.3.2
 */
@Value
public class IndexSearch<T> {

    /**
     * The element to find.
     */
    T element;

    /**
     * The list of all elements.
     */
    List<T> elements;

    /**
     * The indices corresponding to the instances of {@code element} in {@code elements}.
     * @return A non-null, possibly empty list
     */
    public List<Integer> indices() {
        return this.indexMap()
            .entrySet()
            .stream()
            .filter(e -> e.getValue().equals(this.element))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * A map of index-element entries.
     * @return A non-null, possibly empty map
     */
    public Map<Integer, T> indexMap() {
        return IntStream.range(0, this.elements.size())
            .boxed()
            .collect(Collectors.toMap(i -> i, this.elements::get));
    }

}
