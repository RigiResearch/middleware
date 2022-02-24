package com.rigiresearch.middleware.experimentation.infrastructure;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * A value mapper that takes genes and returns actual deployment values for
 * Compute Canada.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class ComputeCanadaChromosome {

    /**
     * Map of supported image flavors (t-shirt sizes).
     */
    private static final Map<String, String> FLAVORS = new HashMap<>();

    static {
        // 1 core
        ComputeCanadaChromosome.FLAVORS.put("1-1.5gb", "p1-1.5gb");
        ComputeCanadaChromosome.FLAVORS.put("1-7.5gb", "c1-7.5gb-36");
        // 2 cores
        ComputeCanadaChromosome.FLAVORS.put("2-3gb", "p2-3gb");
        ComputeCanadaChromosome.FLAVORS.put("2-7.5gb", "c2-7.5gb-36");
        ComputeCanadaChromosome.FLAVORS.put("2-15gb", "c2-15gb-72");
        // 4 cores
        ComputeCanadaChromosome.FLAVORS.put("4-6gb", "p4-6gb");
        ComputeCanadaChromosome.FLAVORS.put("4-15gb", "c4-15gb-144");
        ComputeCanadaChromosome.FLAVORS.put("4-30gb", "c4-30gb-144");
        ComputeCanadaChromosome.FLAVORS.put("4-45gb", "c4-45gb-144");
        // 8 cores
        ComputeCanadaChromosome.FLAVORS.put("8-12gb", "p8-12gb");
        ComputeCanadaChromosome.FLAVORS.put("8-30gb", "c8-30gb-288");
        ComputeCanadaChromosome.FLAVORS.put("8-60gb", "c8-60gb-288");
        ComputeCanadaChromosome.FLAVORS.put("8-90gb", "c8-90gb-288");
        // 16 cores
        ComputeCanadaChromosome.FLAVORS.put("16-60gb", "c16-60gb-576");
        ComputeCanadaChromosome.FLAVORS.put("16-90gb", "c16-90gb-576");
    }

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
    public ComputeCanadaChromosome(final int cpus, final int memory,
        final int nodes) {
        this.cpus = cpus;
        this.memory = memory;
        this.nodes = nodes;
    }

    /**
     * The corresponding Compute Canada flavor name.
     * @return {@code True} if the resulting flavor exists, {@code False} otherwise
     */
    public boolean isSupported() {
        return this.flavor() != null;
    }

    /**
     * A chromosome identifier based on the genes.
     * @return A non-empty non-null string
     */
    public String identifier() {
        return String.format("%d-%s", this.nodes, this.formattedFlavor());
    }

    /**
     * The corresponding Compute Canada flavor name.
     * @return a string or null
     */
    public String flavor() {
        return ComputeCanadaChromosome.FLAVORS.get(this.formattedFlavor());
    }

    /**
     * The flavor name.
     * @return a String composed of the CPUs and memory
     */
    public String formattedFlavor() {
        return String.format("%d-%s", this.actualCpus(), this.formattedMemory());
    }

    /**
     * Translate from the memory gene to the actual deployment value.
     * @return The amount of memory to use
     */
    public String formattedMemory() {
        return String.format(
            "%sgb",
            BigDecimal.valueOf(this.actualMemory())
                .stripTrailingZeros()
                .toString()
        );
    }

    /**
     * Translate from the memory gene to the actual deployment value.
     * @return The amount of memory to use
     */
    public double actualMemory() {
        final double value;
        switch (this.memory) {
            case 1:
                value = 1.5;
                break;
            case 2:
                value = 3.0;
                break;
            case 3:
                value = 4.0;
                break;
            case 4:
                value = 6.0;
                break;
            case 5:
                value = 7.5;
                break;
            case 6:
                value = 8.0;
                break;
            case 7:
                value = 12.0;
                break;
            case 8:
                value = 15.0;
                break;
            case 9:
                value = 16.0;
                break;
            case 10:
                value = 24.0;
                break;
            case 11:
                value = 30.0;
                break;
            case 12:
                value = 32.0;
                break;
            case 13:
                value = 45.0;
                break;
            case 14:
                value = 60.0;
                break;
            case 15:
                value = 90.0;
                break;
            default:
                throw new IllegalArgumentException("Unknown amount of memory");
        }
        return value;
    }

    /**
     * Translate from the CPU gene to the actual deployment value.
     * @return The number of CPUs to use
     */
    public int actualCpus() {
        final int value;
        switch (this.cpus) {
            case 1:
                value = 1;
                break;
            case 2:
                value = 2;
                break;
            case 3:
                value = 4;
                break;
            case 4:
                value = 8;
                break;
            case 5:
                value = 16;
                break;
            default:
                throw new IllegalArgumentException("Unknown number of CPUs");
        }
        return value;
    }

    /**
     * Translate from the nodes gene to the actual deployment value.
     * @return The number of nodes to use
     */
    public int actualNodes() {
        return this.nodes;
    }

}
