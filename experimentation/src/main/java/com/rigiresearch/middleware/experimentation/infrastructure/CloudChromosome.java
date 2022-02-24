package com.rigiresearch.middleware.experimentation.infrastructure;

/**
 * A simple chromosome with memory and cpus.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.11.0
 */
public interface CloudChromosome {

    /**
     * The corresponding cloud's flavor name.
     * @return {@code True} if the resulting flavor exists, {@code False} otherwise
     */
    boolean isSupported();

    /**
     * A chromosome identifier based on the genes.
     * @return A non-empty non-null string
     */
    String identifier();

    /**
     * The corresponding cloud's flavor name.
     * @return a string or null
     */
    String flavor();

    /**
     * The flavor name.
     * @return a String composed of the CPUs and memory
     */
    String formattedFlavor();

    /**
     * Translate from the memory gene to the actual deployment value.
     * @return The amount of memory to use
     */
    String formattedMemory();

    /**
     * Translate from the CPU gene to the actual deployment value.
     * @return The number of CPUs to use
     */
    int actualCpus();

    /**
     * Translate from the nodes gene to the actual deployment value.
     * @return The number of nodes to use
     */
    int actualNodes();

}
