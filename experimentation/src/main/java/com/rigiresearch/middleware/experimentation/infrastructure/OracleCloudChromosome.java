package com.rigiresearch.middleware.experimentation.infrastructure;

/**
 * A chromosome that validates restrictions of the Oracle cloud.
 * TODO Implement a queue for deploying variants concurrently always checking
 *  available resources (Oracle's cores and memory limit).
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.11.0
 */
public class OracleCloudChromosome implements CloudChromosome {

    /**
     * The maximum number of Memory/CPU
     * (i.e., standard-e3-core-ad-count and standard-e3-memory-count).
     */
    public static final int ORACLE_CPU_RAM_LIMIT = 100;

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
        final boolean rate = (double) this.memory / (double) this.cpus > 1.0;
        final boolean limit = OracleCloudChromosome.ORACLE_CPU_RAM_LIMIT >=
            Math.max(this.memory, this.cpus) * this.nodes;
        return rate && limit;
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
