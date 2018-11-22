package com.iota.iri.conf;

/**
 * Configuration for how we perform tip selections. Tip selection is invoked when a client wants to find tips to
 * attach its transactions to. The tips are invoked via random walks that start at a certain point in the tangle.
 * The parameters here affect the length and randomness of this walk.
 */
public interface TipSelConfig extends Config {

     /**
     * @return Descriptions#ALPHA
     */
    double getAlpha();

    interface Descriptions {

        String ALPHA = "Parameter that defines the randomness of the tip selection. " +
                "Should be a number between 0 to infinity, where 0 is most random and infinity is most deterministic.";
    }
}
