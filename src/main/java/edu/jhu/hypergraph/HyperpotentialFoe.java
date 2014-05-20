package edu.jhu.hypergraph;

import edu.jhu.util.semiring.Algebra;

/** Hyperpotential for first-order expectation semiring computations. */
public interface HyperpotentialFoe extends Hyperpotential {

    /** Gets the first-order expectation semiring potential for an edge in a hypergraph. */
    double getScoreFoe(Hyperedge e, Algebra s);
    
}
