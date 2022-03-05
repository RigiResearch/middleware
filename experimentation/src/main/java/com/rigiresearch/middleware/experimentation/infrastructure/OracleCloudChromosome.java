package com.rigiresearch.middleware.experimentation.infrastructure;

/**
 * TODO document this class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.11.0
 */
public class OracleCloudChromosome implements CloudChromosome {

    /**
     * The selected gene for the number of cores.
     */
    private final int cpus;

    /**
     * The selected gene for the amount of RAM.
     */
    private final int memory;

    /**
     * The selected gene for the number of worker nodes.
     */
    private final int nodes;

    /**
     * Default constructor.
     * @param cpus The selected gene for the number of cores
     * @param memory The selected gene for the amount of RAM
     * @param nodes The selected gene for the number of worker nodes
     */
    public OracleCloudChromosome(final int cpus, final int memory,
        final int nodes) {
        this.cpus = cpus;
        this.memory = memory;
        this.nodes = nodes;
    }

    @Override
    public boolean isSupported() {
        return (double) this.memory / (double) this.cpus > 1.0;
    }

    @Override
    public String identifier() {
        return String.format("%d-%d-%s", this.nodes, this.cpus, this.formattedMemory());
    }

    @Override
    public String flavor() {
        return this.identifier();
    }

    @Override
    public String formattedFlavor() {
        return this.identifier();
    }

    @Override
    public String formattedMemory() {
        return String.format("%d", this.memory);
    }

    @Override
    public int actualCpus() {
        return this.cpus;
    }

    @Override
    public int actualNodes() {
        return this.nodes;
    }

}
