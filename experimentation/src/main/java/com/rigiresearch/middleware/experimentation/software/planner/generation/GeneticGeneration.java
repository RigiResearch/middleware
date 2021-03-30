package com.rigiresearch.middleware.experimentation.software.planner.generation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

/**
 * Allows the generation of combination of patterns.
 *
 * @author lfrivera
 * @version $Id$
 * @since 0.1.0
 */
public final class GeneticGeneration<T> {

    /**
     * The default number of chromosome creation iterations following a gene alteration strategy
     */
    private final int DEFAULT_DEPTH = 3;

    /**
     * The initial (basic) architectural structure from the chromosomes will be generated (e.g., [Client, API, Worker]).
     */
    private T[] initialStructure;
    /**
     * The name of patterns to be used for the generation. (e.g., [ProducerConsumer, ProxyCache, LoadBalancer]).
     */
    private T[] patterns;
    /**
     * The genetic generation depth
     */
    private Integer generationDepth;

    private final List<Constraint> constraints;

    /**
     * Constructor of the class.
     *
     * @param initialStructure The initial (basic) architectural structure from the chromosomes will be generated (e.g., [Client, API, Worker]).
     * @param patterns         The name of patterns to be used for the generation. (e.g., [ProducerConsumer, ProxyCache, LoadBalancer]).
     */
    public GeneticGeneration(T[] initialStructure, T[] patterns, Integer generationDepth, final List<Constraint> constraints) {
        this.initialStructure = initialStructure;
        this.patterns = patterns;
        this.generationDepth = generationDepth == null ? DEFAULT_DEPTH : generationDepth;
        this.constraints = constraints;
    }

    /**
     * Allows to generate a population of chromosomes where each of them represent an architecture configuration that involves the application of one or more patterns.
     *
     * @return An array containing the names of the elements in the architectural configuration (read from left to the right).
     * @throws CloneNotSupportedException An exception is thrown in case the chromosome cloning can not be performed.
     */
    public List<Chromosome<T>> generatePopulation() throws CloneNotSupportedException {

        //The population of chromosomes to return
        List<Chromosome<T>> population = null;

        // Getting the initial population
        Chromosome<T>[] initialPopulation = initializePopulation();
        population = new ObjectArrayList<Chromosome<T>>(initialPopulation.length);

        for (Chromosome<T> chromosome : initialPopulation) {
            population.add(chromosome);
        }

        // Performing the crossover
        population = crossover(population);

        return population;
    }

    /**
     * Allows to generate an initial population of chromosomes that includes the application of each pattern (on the possible application points) to the initial structure.
     *
     * @return The initial population of chromosomes.
     */
    private Chromosome<T>[] initializePopulation() {

        int initialPopulationSize = initialStructure.length < 3 ? patterns.length : initialStructure.length * patterns.length;
        Chromosome[] initialPopulation = new Chromosome[initialPopulationSize];
        int chromosomeIndex = 0;
        int applicationPoints = initialStructure.length - 1;
        int chromosomesPerPattern = initialStructure.length < 3 ? initialStructure.length - 1 : initialStructure.length;

        // Navigating through the patterns for the generation
        for (int patternIndex = 0; patternIndex < patterns.length; patternIndex++) {

            // Navigating through the initial architecture components for creating the chromosomes
            for (int papIndex = 0; papIndex < chromosomesPerPattern; papIndex++, chromosomeIndex++) {

                // Creating a new chromosome
                Chromosome<T> chromosome = new Chromosome<T>(initialStructure[0], new ObjectArrayList<Gene<T>>(initialStructure.length - 1));

                // Navigating through the application points for creating the genes
                for (int j = 0; j < applicationPoints; j++) {

                    Gene<T> gene;

                    if (j == papIndex) {

                        gene = new Gene<T>(patterns[patternIndex], new ObjectArrayList<T>());

                    } else if (papIndex < initialStructure.length - 1) {

                        gene = new Gene<T>(null, new ObjectArrayList<T>());

                    } else {

                        gene = new Gene<T>(patterns[patternIndex], new ObjectArrayList<T>());

                    }

                    // Adding the right-side dependency of the application point
                    int indexRightDependency = j + 1;

                    if (indexRightDependency < initialStructure.length) {
                        gene.getRightDependencies().add(initialStructure[indexRightDependency]);
                    }

                    chromosome.getGenes().add(gene);

                }

                initialPopulation[chromosomeIndex] = chromosome;

            }

        }

        return initialPopulation;

    }

    /**
     * Allows to perform the crossover on a population.
     *
     * @param population The population to perform the crossover on.
     * @return Next-generation population.
     * @throws CloneNotSupportedException An exception is thrown in case the chromosome cloning can not be performed.
     */
    private List<Chromosome<T>> crossover(List<Chromosome<T>> population) throws CloneNotSupportedException {

        // Interchanging the genes of the chromosomes generated in the initial population
        interchangeGenes(population);

        // Creating new chromosomes by combining the existing ones with the patterns defined by the user
        generateNewChromosomesByGeneAlteration(population);

        return population;
    }

    /**
     * Allows to interchange the genes of the chromosomes in the current population.
     *
     * @param population The current population.
     */
    private void interchangeGenes(List<Chromosome<T>> population) {

        // Validating the number of elements comprising the systems
        if (initialStructure.length != 3) {

            // TODO: In case the user provides more than 3 initial system elements, the crossover method must include multiple loop blocks to navigate the different pattern application points.
            throw new UnsupportedOperationException("Crossover considering more than 2 application points (more than 3 system elements) has not been implemented yet.");

        } else {

            // Interchanging the genes in the chromosomes to extend the current population
            int i = 0;
            for (Chromosome<T> chromosome1 : population) {

                if (chromosome1.getGenes().get(0).getPatternApplied() != null && chromosome1.getGenes().get(1).getPatternApplied() != null) {

                    if (chromosome1.getGenes().get(0).getPatternApplied().equals(chromosome1.getGenes().get(1).getPatternApplied())) {

                        for (int j = 0; j < population.size(); j++) {

                            if (j != i) {

                                Chromosome<T> chromosome2 = population.get(j);

                                if (chromosome2.getGenes().get(0).getPatternApplied() != null && chromosome2.getGenes().get(1).getPatternApplied() != null) {

                                    if (chromosome2.getGenes().get(0).getPatternApplied().equals(chromosome2.getGenes().get(1).getPatternApplied())) {

                                        Chromosome<T> newChromosome = new Chromosome<T>(initialStructure[0], new ObjectArrayList<Gene<T>>());
                                        newChromosome.getGenes().add(chromosome1.getGenes().get(0));
                                        newChromosome.getGenes().add(chromosome2.getGenes().get(1));

                                        if (isChromosomeFit(newChromosome)) {

                                            population.add(newChromosome);

                                        }

                                    }

                                }

                            }

                        }

                    }

                }
                i++;
            }

        }

    }

    /**
     * Allows to determine whether a chromosome is fit.
     *
     * @param chromosome The chromosome to be evaluated.
     * @return True if the chromosome is fit, false otherwise.
     */
    private boolean isChromosomeFit(Chromosome<T> chromosome) {
        return this.constraints.stream()
            .allMatch(constraint -> constraint.holds(chromosome));
    }

    /**
     * Allows to predict whether a chromosome will be fit using the genes that will comprised it.
     *
     * @param gene The chromosome to be evaluated.
     * @return True if the chromosome is fit, false otherwise.
     */
    private boolean isChromosomeFit(Gene<T> gene, T patternToApply) {

        boolean response = true;

        if(patternToApply.equals(gene.getPatternApplied())) {

            response = false;

        }

        return response;


    }


    /**
     * Allows to create new chromosomes by combining the existing ones with the patterns defined by the user.
     *
     * @param population The current population of chromosomes.
     * @throws CloneNotSupportedException An exception is thrown in case the Gene cloning can not be performed.
     */
    private void generateNewChromosomesByGeneAlteration(List<Chromosome<T>> population) throws CloneNotSupportedException {

        // TODO: The stop condition might be improved
        for (int i = 0; i < generationDepth; i++) {

            population.addAll(alterGenes(population));

        }
    }

    /**
     * Allows to create new chromosomes by altering genes.
     *
     * @param population The current population.
     * @return New chromosomes created.
     * @throws CloneNotSupportedException An exception is thrown in case the chromosome cloning can not be performed.
     */
    private List<Chromosome<T>> alterGenes(List<Chromosome<T>> population) throws CloneNotSupportedException {

        List<Chromosome<T>> newChromosomes = new ObjectArrayList<Chromosome<T>>();

        // Navigating through the current population
        for (Chromosome<T> currentChromosome : population) {

            // Navigating through the genes of each chromosome
            for (int geneIndex = 0; geneIndex < currentChromosome.getGenes().size(); geneIndex++) {

                Gene<T> currentGene = currentChromosome.getGenes().get(geneIndex);

                // Navigating through the patterns defined by the user
                for (T patternToApply : patterns) {

                    // Pre-validation based on the existing genes of a chromosoe (avoid neighbour-type duplicates)
                    if(isChromosomeFit(currentGene,patternToApply)) {

                        Chromosome<T> newChromosome = new Chromosome<T>(currentChromosome);
                        newChromosome.getGenes().set(geneIndex, alterGene(currentGene, patternToApply));

                        if (isChromosomeFit(newChromosome)) {

                            newChromosomes.add(newChromosome);

                        }

                    }


                }

            }

        }

        return newChromosomes;

    }

    /**
     * Allows to alter a gene by applying a new pattern and moving the existing one to the right dependencies lists.
     *
     * @param gene           The original gene.
     * @param patternToApply The pattern to apply.
     * @return Altered gene.
     * @throws CloneNotSupportedException An exception is thrown in case the Gene cloning can not be performed.
     */
    private Gene<T> alterGene(Gene<T> gene, T patternToApply) throws CloneNotSupportedException {

        Gene<T> newGene = new Gene<T>(gene);
        T currentAppliedPattern = newGene.getPatternApplied();
        newGene.setPatternApplied(patternToApply);
        if(currentAppliedPattern != null) {
            newGene.getRightDependencies().add(0, currentAppliedPattern);
        }

        return newGene;

    }

}
