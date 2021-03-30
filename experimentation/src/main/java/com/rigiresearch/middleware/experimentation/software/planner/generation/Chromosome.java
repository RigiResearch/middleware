package com.rigiresearch.middleware.experimentation.software.planner.generation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a chromosome, that is, a list of genes.
 *
 * @author lfrivera
 * @version $Id$
 * @since 0.1.0
 */
public final class Chromosome<T> implements Serializable {

    /**
     * The initial gene of the chromosome.
     */
    private T initialGene;

    /**
     * The list of genes for the chromosome.
     */
    private List<Gene<T>> genes;

    /**
     * Constructor of the class.
     *
     * @param initialGene The initial gene of the chromosome.
     * @param genes       The list of genes for the chromosome.
     */
    public Chromosome(T initialGene, List<Gene<T>> genes) {
        this.initialGene = initialGene;
        this.genes = genes;
    }

    /**
     * Copy constructor.
     *
     * @param chromosomeToCopy The gene to be copied (cloned).
     */
    public Chromosome(Chromosome<T> chromosomeToCopy) {

        Chromosome<T> newChromosome = (Chromosome<T>) StreamCloner.getInstance().clone(chromosomeToCopy);
        this.initialGene = newChromosome.getInitialGene();
        this.genes = newChromosome.getGenes();

    }

    public List<T> allComponents() {
        final List<T> list = new ObjectArrayList<>();
        list.add(this.initialGene);
        list.addAll(this.genes.stream()
            .map(Gene::components)
            .flatMap(List::stream)
            .collect(Collectors.toList()));
        return list;
    }

    @Override
    public String toString() {

        String string = null;

        if (genes != null) {

            if (!genes.isEmpty()) {

                StringBuilder strbuild = new StringBuilder();
                boolean firstTime = true;

                for (Gene gene : genes) {

                    if (firstTime) {
                        firstTime = false;
                    } else {

                        strbuild.append("|");

                    }
                    strbuild.append(gene.toString());
                }

                string = strbuild.toString();

            }

        }

        return string;
    }

    /**
     * Allows obtaining a string that contains a readable version of the chromosome.
     *
     * @param regex The separator between ech element in the chromosome.
     * @return A string containing a readable version of the chromosome.
     */
    public String toString(String regex) {

        StringBuilder strbuild;

        if (initialGene == null) {

            strbuild = new StringBuilder("");

        } else {

            strbuild = new StringBuilder(initialGene.toString());

        }

        if (genes != null) {

            for (Gene<T> gene : genes) {

                strbuild.append(regex);
                strbuild.append(gene.toString(regex));
            }

        }

        String preResult = strbuild.toString();
        strbuild = new StringBuilder("");
        boolean firstTime = true;

        for (String s : preResult.split(regex)) {

            if (firstTime) {

                strbuild.append(s);
                firstTime = false;

            } else {

                if (!s.isEmpty()) {

                    strbuild.append(regex);
                    strbuild.append(s);

                }

            }

        }


        return strbuild.toString();
    }

    public T getInitialGene() {
        return initialGene;
    }

    public void setInitialGene(T initialGene) {
        this.initialGene = initialGene;
    }

    public List<Gene<T>> getGenes() {
        return genes;
    }

    public void setGenes(List<Gene<T>> genes) {
        this.genes = genes;
    }

}
