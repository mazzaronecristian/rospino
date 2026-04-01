package org.oristool.rospino.computingNodes;

import org.oristool.models.stpn.trees.StochasticTransitionFeature;

import java.math.BigDecimal;

public class Delay {
    private final BigDecimal eft;
    private final BigDecimal lft;

    public Delay(BigDecimal eft, BigDecimal lft) {
        this.eft = eft;
        this.lft = lft;
    }

    public Delay(double eft, double lft){
        this.eft = BigDecimal.valueOf(eft);
        this.lft = BigDecimal.valueOf(lft);
    }

    public BigDecimal getEft() {
        return eft;
    }

    public BigDecimal getLft() {
        return lft;
    }

    public static Delay getInfiniteDelay() {
        return new Delay(BigDecimal.valueOf(Double.MAX_VALUE), BigDecimal.valueOf(Double.MAX_VALUE));
    }

    public StochasticTransitionFeature toUniformInstance() {
        return StochasticTransitionFeature.newUniformInstance(eft, lft);
    }

}