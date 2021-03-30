package com.rigiresearch.middleware.experimentation.software.planner.generation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.Serializable;
import java.util.List;

/**
 * Represents a gene, that is, a pattern application point (PAP) and its right side dependency.
 *
 * @author lfrivera
 * @version $Id$
 * @since 0.1.0
 */
public final class Gene<T> implements Serializable{

    /**
     * Represents the pattern being applied in an application point (e.g.CLI -> PROX -> API).
     */
    private T patternApplied;

    /**
     * Represents the right-hand dependencies (destination) of the pattern being applied.
     */
    private List<T> rightDependencies;

    /**
     * Constructor of the class.
     *
     * @param patternApplied    Represents the pattern being applied in an application point (e.g.CLI -> PROX -> API).
     * @param rightDependencies Represents the right-hand dependencies (destination) of the pattern being applied.
     */
    public Gene(T patternApplied, List<T> rightDependencies) {
        this.patternApplied = patternApplied;
        this.rightDependencies = rightDependencies;
    }

    /**
     * Copy constructor.
     *
     * @param geneToCopy The gene to be copied (cloned).
     */
    public Gene(Gene<T> geneToCopy) {

       Gene<T> newGene = (Gene<T>) StreamCloner.getInstance().clone(geneToCopy);
       this.patternApplied = newGene.getPatternApplied();
       this.rightDependencies = newGene.getRightDependencies();

    }

    public List<T> components() {
        final List<T> list = new ObjectArrayList<>();
        if (this.patternApplied != null) {
            list.add(this.patternApplied);
        }
        list.addAll(this.rightDependencies);
        return list;
    }

    @Override
    public String toString() {

        StringBuilder strbuild = null;

        if (patternApplied != null) {

            strbuild = new StringBuilder(patternApplied.toString());

        } else {

            strbuild = new StringBuilder("");


        }

        strbuild.append(";");

        if (rightDependencies != null) {

            if (!rightDependencies.isEmpty()) {

                boolean firstTime = true;

                for (T dependency : rightDependencies) {

                    if (firstTime) {

                        firstTime = false;

                    } else {

                        strbuild.append(",");

                    }

                    strbuild.append(dependency.toString());

                }

            }

        }

        return strbuild.toString();
    }

    /**
     * Allows obtaining a string that contains a readable version of the gene.
     *
     * @param regex The separator between ech element in the gene.
     * @return A string containing a readable version of the gene.
     */
    public String toString(String regex) {

        StringBuilder strbuild;

        if (patternApplied == null) {
            strbuild = new StringBuilder("");

        } else {

            strbuild = new StringBuilder(patternApplied.toString());

        }

        if (rightDependencies != null) {


            for (T dependency : rightDependencies) {

                strbuild.append(regex);
                strbuild.append(dependency.toString());

            }

        }

        return strbuild.toString();
    }

    public T getPatternApplied() {
        return patternApplied;
    }

    public void setPatternApplied(T patternApplied) {
        this.patternApplied = patternApplied;
    }

    public List<T> getRightDependencies() {
        return rightDependencies;
    }

    public void setRightDependencies(List<T> rightDependencies) {
        this.rightDependencies = rightDependencies;
    }
}
