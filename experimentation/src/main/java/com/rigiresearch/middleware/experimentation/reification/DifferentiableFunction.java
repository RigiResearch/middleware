package com.rigiresearch.middleware.experimentation.reification;

/**
 * A simple differentiable function.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public interface DifferentiableFunction {

    /**
     * Computes f(x1, x2, ..., xn).
     * @param parameters The parameters of this function
     * @return The function evaluated on the given parameters.
     */
    double value(double[] parameters);

    /**
     * Computes ∇f(x1, x2, ..., xn).
     * @param parameters The parameters of this function
     * @return The gradient evaluated on the given parameters.
     */
    double[] gradient(double[] parameters);

}
