package org.oristool.rospino.computingNodes;

import org.oristool.models.stpn.trees.StochasticTransitionFeature;

import java.math.BigDecimal;

public class Delay {
    private static final BigDecimal PSEUDO_INFINITY = new BigDecimal("1E+999999999");
    private final BigDecimal eft;
    private final BigDecimal lft;

    public Delay(BigDecimal eft, BigDecimal lft) {
        this.eft = eft;
        this.lft = lft;
    }

    public static BigDecimal getInfiniteDelay() {
        return PSEUDO_INFINITY;
    }

    public StochasticTransitionFeature toUniformInstance() {
        return StochasticTransitionFeature.newUniformInstance(eft, lft);
    }
}